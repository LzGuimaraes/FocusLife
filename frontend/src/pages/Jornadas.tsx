import { useCallback, useEffect, useRef, useState, type ReactNode } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { isAxiosError } from 'axios';
import { toast } from 'sonner';
import api from '../api/api';
import Layout from '../components/Layout';
import Modal from '../components/Modal';
import LofiPlayer from '../components/LofiPlayer';
import EstudoTimer from '../components/EstudoTimer';
import { Input, Select, TextArea } from '../components/Form';
import { PageHeader, Spinner, EmptyState, ErrorState } from '../components/UI';
import { type Atividade, type Material, type Progresso, type Estudo, type StatusAtividade, statusNomes, numero, dataLocal, hojeCuiaba } from '../types/jornadas';
import './Jornadas.css';

const dias = [['MONDAY','Seg'],['TUESDAY','Ter'],['WEDNESDAY','Qua'],['THURSDAY','Qui'],['FRIDAY','Sex'],['SATURDAY','Sáb'],['SUNDAY','Dom']];
const unidades = ['aulas','exercícios','páginas','horas','unidades','%'];
interface Formulario { titulo: string; descricao: string; inicio: string; prazo: string; unidade: string; fracionavel: boolean; total: string; saldoInicial: string; saldoData: string; status: StatusAtividade; prioridade: string; horario: string; agrupamento: string; diasSemana: string[] }
const novoForm = (): Formulario => ({ titulo: '', descricao: '', inicio: hojeCuiaba(), prazo: '', unidade: 'unidades', fracionavel: false, total: '1', saldoInicial: '0', saldoData: '', status: 'NAO_INICIADA', prioridade: '', horario: '', agrupamento: '', diasSemana: [] });
const erroMensagem = (e: unknown) => isAxiosError(e) ? e.response?.data?.message || 'Não foi possível salvar. Tente novamente.' : 'Não foi possível completar a operação.';
const atencao = (a: Atividade) => ['NAO_INICIADA','EM_ANDAMENTO'].includes(a.status) && (a.indicadores.posicao === 'ATRASADA' || ['VENCIDA','HOJE'].includes(a.indicadores.situacaoPrazo) || ['FORA_DO_PRAZO','SEM_DIAS_DISPONIVEIS'].includes(a.indicadores.tendencia));

export function ResumoJornadas() {
  const [dados, setDados] = useState<Atividade[] | null>(null);
  const [erro, setErro] = useState(false);
  const navigate = useNavigate();
  useEffect(() => { const c = new AbortController(); api.get<Atividade[]>('/jornadas', { signal: c.signal }).then(r => setDados(r.data)).catch(() => { if (!c.signal.aborted) setErro(true); }); return () => c.abort(); }, []);
  return <section className="jornada-resumo-dashboard"><h2>Suas jornadas</h2>{erro ? <p role="alert">Não foi possível carregar o resumo.</p> : dados === null ? <p>Carregando atividades…</p> : <p>{dados.filter(a => a.status === 'EM_ANDAMENTO').length} em andamento · {dados.filter(atencao).length} precisam de atenção · {dados.filter(a => a.status === 'CONCLUIDA').length} concluídas</p>}<button className="jornada-button" onClick={() => navigate('/jornadas')}>Abrir Jornadas →</button></section>;
}

export default function Jornadas({ admin = false }: { admin?: boolean }) {
  const { id, usuarioId } = useParams();
  const navigate = useNavigate();
  const [atividades, setAtividades] = useState<Atividade[]>([]);
  const [materiais, setMateriais] = useState<Material[]>([]);
  const [detalhe, setDetalhe] = useState<Atividade | null>(null);
  const [loading, setLoading] = useState(true);
  const [erro, setErro] = useState(false);
  const [detalheErro, setDetalheErro] = useState(false);
  const detalheRequest = useRef(0);
  const [busy, setBusy] = useState(false);
  const trava = useRef(false);
  const [busca, setBusca] = useState('');
  const [status, setStatus] = useState('');
  const [prazo, setPrazo] = useState('');
  const [grupo, setGrupo] = useState('');
  const [editar, setEditar] = useState<Atividade | null | undefined>(undefined);
  const [form, setForm] = useState<Formulario>(novoForm);
  const [progresso, setProgresso] = useState<{ atividade: Atividade; item?: Progresso } | null>(null);
  const [lancamento, setLancamento] = useState({ data: hojeCuiaba(), modo: 'INCREMENTO', quantidade: '', observacao: '', motivacao: '', requestId: '' });
  const [link, setLink] = useState<Material | null | undefined>(undefined);
  const [linkForm, setLinkForm] = useState({ titulo: '', url: '', descricao: '', atividadeId: '' });
  const [estudo, setEstudo] = useState<Estudo | null | undefined>(undefined);
  const [estudoForm, setEstudoForm] = useState({ nome: '', data: hojeCuiaba(), duracaoMin: '0', notas: '' });
  const prefixo = admin ? `/admin/users/${usuarioId}/jornadas` : '/jornadas';
  const carregar = useCallback(async () => {
    setErro(false);
    try {
      const r = await api.get<Atividade[]>(prefixo); setAtividades(r.data);
      if (!admin) { const m = await api.get<Material[]>('/jornadas/materiais'); setMateriais(m.data); }
    } catch { setErro(true); } finally { setLoading(false); }
  }, [prefixo, admin]);
  useEffect(() => { setLoading(true); void carregar(); }, [carregar]);
  const carregarDetalhe = useCallback(async () => {
    const request = ++detalheRequest.current;
    if (!id) { setDetalhe(null); setDetalheErro(false); return; }
    setDetalheErro(false);
    try { const r = await api.get<Atividade>(`${prefixo}/${id}`); if (request === detalheRequest.current) setDetalhe(r.data); }
    catch { if (request === detalheRequest.current) setDetalheErro(true); }
  }, [id, prefixo]);
  useEffect(() => { setDetalhe(null); void carregarDetalhe(); }, [carregarDetalhe]);
  const executar = async (acao: () => Promise<unknown>, sucesso = 'Salvo!') => {
    if (trava.current) return;
    trava.current = true; setBusy(true);
    try { await acao(); toast.success(sucesso); await carregar(); if (id) await carregarDetalhe(); }
    catch (e) { toast.error(erroMensagem(e)); if (isAxiosError(e) && e.response?.status === 409) { await carregar(); await carregarDetalhe(); } }
    finally { trava.current = false; setBusy(false); }
  };
  const abrirEdicao = (a: Atividade | null) => {
    setEditar(a);
    setForm(a ? { titulo: a.titulo, descricao: a.descricao || '', inicio: a.inicio || '', prazo: a.prazo || '', unidade: a.unidade || 'unidades', fracionavel: a.fracionavel, total: a.total?.toString() || '', saldoInicial: a.saldoInicial.toString(), saldoData: a.saldoData || '', status: a.status, prioridade: a.prioridade || '', horario: a.horario?.slice(0,5) || '', agrupamento: a.agrupamento || '', diasSemana: a.diasSemana } : novoForm());
  };
  const salvar = () => executar(async () => {
    if (!form.titulo.trim()) { throw new Error('Título obrigatório'); }
    const payload = { ...form, version: editar?.version, inicio: form.inicio || null, prazo: form.prazo || null, total: form.total === '' ? null : Number(form.total), saldoInicial: Number(form.saldoInicial), saldoData: form.saldoData || null, horario: form.horario || null };
    if (editar) await api.put(`/jornadas/${editar.id}`, payload); else await api.post('/jornadas', payload);
    setEditar(undefined);
  });
  const abrirProgresso = (a: Atividade, item?: Progresso) => {
    setProgresso({ atividade: a, item }); setLancamento({ data: item?.data || hojeCuiaba(), modo: item?.modo || (a.unidade === 'páginas' ? 'ACUMULADO' : 'INCREMENTO'), quantidade: item?.quantidade.toString() || '', observacao: item?.observacao || '', motivacao: item?.motivacao?.toString() || '', requestId: item?.requestId || crypto.randomUUID() });
  };
  const salvarProgresso = () => executar(async () => {
    if (!progresso) return;
    const url = `/jornadas/${progresso.atividade.id}/progresso${progresso.item ? `/${progresso.item.id}` : ''}`;
    const p = { ...lancamento, version: progresso.atividade.version, quantidade: Number(lancamento.quantidade), motivacao: lancamento.motivacao ? Number(lancamento.motivacao) : null };
    if (progresso.item) await api.put(url,p); else await api.post(url,p);
    setProgresso(null);
  });
  const abrirLink = (m: Material | null) => { setLink(m); setLinkForm({ titulo: m?.titulo || '', url: m?.url || '', descricao: m?.descricao || '', atividadeId: m?.atividadeId?.toString() || detalhe?.id.toString() || '' }); };
  const salvarLink = () => executar(async () => { const p = { ...linkForm, version: link?.version, atividadeId: linkForm.atividadeId ? Number(linkForm.atividadeId) : null }; if (link) await api.put(`/jornadas/materiais/${link.id}`, p); else await api.post('/jornadas/materiais',p); setLink(undefined); });
  const abrirEstudo = (e: Estudo | null) => { setEstudo(e); setEstudoForm({ nome: e?.nome || '', data: e?.data || hojeCuiaba(), duracaoMin: e?.duracaoMin.toString() || '0', notas: e?.notas || '' }); };
  const salvarEstudo = () => executar(async () => { if (!detalhe) return; const url = `/jornadas/${detalhe.id}/estudos${estudo ? `/${estudo.id}` : ''}`; const p = { ...estudoForm, duracaoMin: Number(estudoForm.duracaoMin) }; if (estudo) await api.put(url,p); else await api.post(url,p); setEstudo(undefined); });
  const confirmar = (texto: string, acao: () => Promise<unknown>) => { toast(texto, { action: { label: 'Excluir', onClick: () => void executar(acao, 'Excluído!') }, cancel: { label: 'Cancelar', onClick: () => {} } }); };
  const grupos = [...new Set(atividades.map(a => a.agrupamento).filter((g): g is string => !!g))];
  const filtradas = atividades.filter(a => `${a.titulo} ${a.descricao || ''}`.toLocaleLowerCase('pt-BR').includes(busca.toLocaleLowerCase('pt-BR')) && (!status || a.status === status) && (!grupo || a.agrupamento === grupo) && (!prazo || (prazo === 'ATENCAO' ? atencao(a) : a.indicadores.situacaoPrazo === prazo)));
  const listaLinks = (lista: Material[]) => lista.length === 0 ? <p className="jornada-muted">Nenhum material salvo.</p> : <ul className="jornada-materiais">{lista.map(m => <li key={m.id}><div><a href={m.url} target="_blank" rel="noopener noreferrer">{m.titulo} ↗</a>{m.descricao && <p>{m.descricao}</p>}<small>{atividades.find(a => a.id === m.atividadeId)?.titulo || 'Recurso geral'}</small></div>{!admin && <div className="jornada-actions"><button onClick={() => abrirLink(m)}>Editar</button><button onClick={() => confirmar('Excluir este material?', () => api.delete(`/jornadas/materiais/${m.id}?version=${m.version}`))}>Excluir</button></div>}</li>)}</ul>;

  return <Layout><div className="jornadas">
    <PageHeader icon="🧭" title={admin ? `Jornadas · usuário #${usuarioId}` : 'Jornadas'} subtitle="Um lugar para planejar, realizar e acompanhar suas atividades." {...(!admin ? { actionLabel: 'Nova atividade', onAction: () => abrirEdicao(null) } : {})} />
    {admin && <p>Consulta administrativa, somente leitura. <button onClick={() => navigate(`/admin/usuarios/${usuarioId}`)}>Voltar ao usuário</button></p>}
    {id ? <>
      <button className="jornada-button" onClick={() => navigate(admin ? `/admin/usuarios/${usuarioId}/jornadas` : '/jornadas')}>← Todas as atividades</button>
      {detalheErro ? <ErrorState text="Não foi possível carregar a atividade." onRetry={carregarDetalhe} /> : !detalhe ? <Spinner text="Carregando atividade…" /> : <>
        <article className="jornada-card jornada-detail"><div className="jornada-top"><div><span className="jornada-badge">{statusNomes[detalhe.status]}</span><h2>{detalhe.titulo}</h2><p className="jornada-muted">{detalhe.agrupamento} · {dataLocal(detalhe.inicio)} → {dataLocal(detalhe.prazo)}</p></div>{!admin && <button className="jornada-button" onClick={() => abrirEdicao(detalhe)}>Editar atividade</button>}</div>
          {detalhe.descricao && <p>{detalhe.descricao}</p>}<Metricas atividade={detalhe} />
          {!admin && <div className="jornada-actions"><button className="jornada-button primary" disabled={busy} onClick={() => abrirProgresso(detalhe)}>Registrar progresso</button><button className="jornada-button danger" disabled={busy} onClick={() => confirmar('Excluir a atividade, todo o progresso e os estudos? Os materiais serão mantidos como recursos gerais.', async () => { await api.delete(`/jornadas/${detalhe.id}?version=${detalhe.version}`); navigate('/jornadas'); })}>Excluir atividade</button></div>}
        </article>
        <section className="jornada-card"><h3>Progresso real e planejado</h3><Grafico atividade={detalhe} /><p className="jornada-muted">Fotografia ao fim de {dataLocal(detalhe.referencia)} · America/Cuiaba. Hoje entra no observado; oportunidades futuras começam amanhã. {detalhe.indicadores.baseDias === 'DIAS_PLANEJADOS' ? 'Cálculo por dias planejados.' : 'Cálculo por dias corridos.'}</p></section>
        <section className="jornada-card"><h3>Histórico de progresso</h3><p className="jornada-muted">Saldo inicial: {numero(detalhe.saldoInicial)} {detalhe.unidade} {detalhe.saldoData ? `ao fim de ${dataLocal(detalhe.saldoData)}` : '· referência desconhecida'}. Não conta como produção observada.</p>
          {detalhe.percentualLegado != null && <p>Percentual original da meta: {numero(detalhe.percentualLegado)}%. Para mudar a medida para páginas ou aulas, informe as quantidades reais.</p>}
          {detalhe.historico.length === 0 ? <p>Sem lançamentos. Registre seu primeiro avanço.</p> : <div className="jornada-table-wrap"><table><caption className="sr-only">Lançamentos de {detalhe.titulo}</caption><thead><tr><th>Data</th><th>Registro</th><th>Observação</th><th>Motivação</th>{!admin && <th>Ações</th>}</tr></thead><tbody>{detalhe.historico.map(p => <tr key={p.id}><td>{dataLocal(p.data)}</td><td>{p.modo === 'INCREMENTO' ? '+' : 'Acumulado: '}{numero(p.quantidade)} {detalhe.unidade}</td><td>{p.observacao || '—'}</td><td>{p.motivacao == null ? '—' : `${p.motivacao}/5`}</td>{!admin && <td><button onClick={() => abrirProgresso(detalhe,p)}>Corrigir</button> <button onClick={() => confirmar('Excluir este lançamento?', () => api.delete(`/jornadas/${detalhe.id}/progresso/${p.id}?version=${detalhe.version}`))}>Excluir</button></td>}</tr>)}</tbody></table></div>}
        </section>
        <section className="jornada-card"><h3>Tempo de estudo e anotações</h3><p className="jornada-muted">Tempo dedicado e produção são medidas independentes. Pausas não estendem o prazo; dias pausados permanecem na média observada ao retomar.</p>
          <EstudoTimer atividade={detalhe} readonly={admin} busy={busy} executar={executar} />
          <p>Tempo registrado manualmente: {numero(detalhe.estudos.reduce((s,e) => s+e.duracaoMin,0))} min (separado do cronômetro para evitar duplicidade).</p>
          {!admin && <button className="jornada-button" onClick={() => abrirEstudo(null)}>Registrar sessão e anotações</button>}
          {detalhe.estudos.map(e => <article className="jornada-nota" key={e.id}><h4>{e.nome}</h4><small>{dataLocal(e.data)} · {e.duracaoMin} min</small><p>{e.notas}</p>{!admin && <div className="jornada-actions"><button onClick={() => abrirEstudo(e)}>Editar sessão</button><button onClick={() => confirmar('Excluir esta sessão e suas anotações?', () => api.delete(`/jornadas/${detalhe.id}/estudos/${e.id}`))}>Excluir sessão</button></div>}</article>)}
          {detalhe.sessoes.length > 0 && <details><summary>Histórico do cronômetro ({detalhe.sessoes.length})</summary><ul>{detalhe.sessoes.map(s => <li key={s.id}>{dataLocal(s.inicio.slice(0,10))} · {numero(s.duracaoSegundos/60)} min · {s.fim ? 'Encerrada' : 'Em andamento'}</li>)}</ul></details>}
          {!admin && <LofiPlayer />}
        </section>
        <section className="jornada-card"><div className="jornada-top"><h3>Links e materiais desta atividade</h3>{!admin && <button className="jornada-button" onClick={() => abrirLink(null)}>Adicionar link</button>}</div>{listaLinks(detalhe.materiais)}</section>
      </>}
    </> : <>
      {loading ? <Spinner text="Carregando jornadas…" /> : erro ? <ErrorState text="Não foi possível carregar suas jornadas e materiais." onRetry={carregar} /> : <>
        <div className="jornada-summary"><Resumo valor={atividades.filter(a => a.status === 'EM_ANDAMENTO').length} rotulo="Em andamento" /><Resumo valor={atividades.filter(atencao).length} rotulo="Precisam de atenção" /><Resumo valor={atividades.filter(a => a.status === 'CONCLUIDA').length} rotulo="Concluídas" /></div>
        <div className="jornada-filters"><Input label="Buscar atividade" value={busca} onChange={e => setBusca(e.target.value)} placeholder="Título ou descrição" /><Select label="Status" value={status} onChange={e => setStatus(e.target.value)}><option value="">Todos</option>{Object.entries(statusNomes).map(([v,n]) => <option key={v} value={v}>{n}</option>)}</Select><Select label="Situação do prazo" value={prazo} onChange={e => setPrazo(e.target.value)}><option value="">Todas</option><option value="ATENCAO">Precisa de atenção</option><option value="VENCIDA">Prazo vencido</option><option value="HOJE">Prazo hoje</option><option value="NO_PRAZO">Prazo futuro</option><option value="SEM_PRAZO">Sem prazo</option></Select><Select label="Agrupamento" value={grupo} onChange={e => setGrupo(e.target.value)}><option value="">Todos</option>{grupos.map(g => <option key={g}>{g}</option>)}</Select></div>
        {filtradas.length === 0 ? <EmptyState icon="🧭" title="Nenhuma atividade encontrada" text={atividades.length ? 'Ajuste a busca ou os filtros.' : 'Comece por um livro, curso, exercícios ou uma atividade simples.'} {...(!admin ? { actionLabel: 'Nova atividade', onAction: () => abrirEdicao(null) } : {})} /> : <div className="jornada-grid">{filtradas.map(a => <article className="jornada-card" key={a.id}><div className="jornada-top"><span className="jornada-badge">{statusNomes[a.status]}</span>{a.prioridade && <small>Prioridade {a.prioridade}</small>}</div><h2><button className="jornada-title" onClick={() => navigate(admin ? `/admin/usuarios/${usuarioId}/jornadas/${a.id}` : `/jornadas/${a.id}`)}>{a.titulo} →</button></h2><p className="jornada-muted">{dataLocal(a.inicio)} → {dataLocal(a.prazo)}{a.agrupamento && ` · ${a.agrupamento}`}</p><Metricas atividade={a} compacto /><div className="jornada-actions">{!admin && <><button className="jornada-button primary" disabled={busy || ['CONCLUIDA','CANCELADA'].includes(a.status)} onClick={() => abrirProgresso(a)}>Registrar progresso</button>{a.total === 1 && a.indicadores.realizado === 0 && !['CANCELADA'].includes(a.status) && <button className="jornada-button" disabled={busy} onClick={() => void executar(() => api.post(`/jornadas/${a.id}/progresso`, { version: a.version, requestId: crypto.randomUUID(), data: hojeCuiaba(), modo: 'ACUMULADO', quantidade: 1 }), 'Atividade concluída!')}>Concluir</button>}<button className="jornada-button" onClick={() => abrirEdicao(a)}>Editar</button></>}<button className="jornada-button" onClick={() => navigate(admin ? `/admin/usuarios/${usuarioId}/jornadas/${a.id}` : `/jornadas/${a.id}`)}>Ver detalhes</button></div></article>)}</div>}
        {!admin && <section className="jornada-card"><div className="jornada-top"><div><h2>Links e materiais</h2><p className="jornada-muted">Guarde recursos gerais ou vinculados a uma atividade.</p></div><button className="jornada-button" onClick={() => abrirLink(null)}>Adicionar link</button></div>{listaLinks(materiais)}</section>}
      </>}
    </>}
    <Modal open={editar !== undefined} onClose={() => !busy && setEditar(undefined)} title={editar ? 'Editar atividade' : 'Nova atividade'} onSubmit={salvar} submitLabel={busy ? 'Salvando…' : 'Salvar atividade'} submitDisabled={busy} width="720px">
      <Input label="Título" required maxLength={255} value={form.titulo} onChange={e => setForm({...form,titulo:e.target.value})} />
      <TextArea label="Descrição (opcional)" maxLength={10000} value={form.descricao} onChange={e => setForm({...form,descricao:e.target.value})} />
      <div className="jornada-form-grid"><Input label="Data de início" type="date" value={form.inicio} onChange={e => setForm({...form,inicio:e.target.value})} /><Input label="Prazo previsto" type="date" min={form.inicio || undefined} value={form.prazo} onChange={e => setForm({...form,prazo:e.target.value})} /></div>
      <p className="jornada-muted">Sem datas ou quantidade, a atividade continua acessível, mas as projeções ficam indisponíveis.</p>
      <div className="jornada-form-grid"><label className="jornada-label">Unidade de medida<input list="jornada-unidades" required maxLength={80} value={form.unidade} onChange={e => setForm({...form,unidade:e.target.value,fracionavel:['horas','%'].includes(e.target.value)})} /><datalist id="jornada-unidades">{unidades.map(u => <option key={u}>{u}</option>)}</datalist></label><Input label={form.unidade === 'páginas' ? 'Total de páginas' : 'Quantidade total desejada'} type="number" min={form.fracionavel ? "0.0001" : "1"} step={form.fracionavel ? '0.0001' : '1'} value={form.total} onChange={e => setForm({...form,total:e.target.value})} /></div>
      <label className="jornada-check"><input type="checkbox" checked={form.fracionavel} onChange={e => setForm({...form,fracionavel:e.target.checked})} />Permitir quantidades fracionárias</label>
      <div className="jornada-form-grid"><Input label={form.unidade === 'páginas' ? 'Página atual / saldo inicial' : 'Quantidade já realizada / saldo inicial'} type="number" required min="0" max={form.total || undefined} step={form.fracionavel ? '0.0001' : '1'} value={form.saldoInicial} onChange={e => { const saldo=e.target.value; setForm({...form,saldoInicial:saldo,status:form.total && Number(saldo)===Number(form.total)?'CONCLUIDA':Number(saldo)>0?'EM_ANDAMENTO':form.status==='CONCLUIDA'?'NAO_INICIADA':form.status}); }} /><Input label="Data de referência do saldo (fim do dia)" type="date" max={hojeCuiaba()} required={Number(form.saldoInicial)>0} value={form.saldoData} onChange={e => setForm({...form,saldoData:e.target.value})} /></div>
      <p className="jornada-muted">O saldo inicial não cria histórico anterior. Novos lançamentos devem ocorrer depois dessa data. Para começar do zero, deixe a referência vazia: será a véspera do início. Corrija avanços pelo histórico após o cadastro.</p>
      <div className="jornada-form-grid"><Select label="Status" value={form.status} onChange={e => setForm({...form,status:e.target.value as StatusAtividade})}>{Object.entries(statusNomes).map(([v,n]) => <option value={v} key={v}>{n}</option>)}</Select><Select label="Prioridade (opcional)" value={form.prioridade} onChange={e => setForm({...form,prioridade:e.target.value})}><option value="">Sem prioridade</option><option value="baixa">Baixa</option><option value="media">Média</option><option value="alta">Alta</option></Select></div>
      <div className="jornada-form-grid"><Input label="Curso, assunto ou projeto (opcional)" maxLength={255} list="jornada-grupos" value={form.agrupamento} onChange={e => setForm({...form,agrupamento:e.target.value})} /><datalist id="jornada-grupos">{grupos.map(g => <option key={g}>{g}</option>)}</datalist><Input label="Horário do lembrete (opcional)" type="time" value={form.horario} onChange={e => setForm({...form,horario:e.target.value})} /></div>
      <fieldset className="jornada-days"><legend>Dias planejados (opcional; vazio = todos os dias)</legend>{dias.map(([d,n]) => <label key={d}><input type="checkbox" checked={form.diasSemana.includes(d)} onChange={e => setForm({...form,diasSemana:e.target.checked?[...form.diasSemana,d]:form.diasSemana.filter(v => v!==d)})} />{n}</label>)}</fieldset>
      <p className="jornada-muted">Com horário, o lembrete é enviado na primeira verificação após esse horário. Sem horário, atividades com prazo hoje recebem o lembrete diário.</p>
    </Modal>
    <Modal open={!!progresso} onClose={() => !busy && setProgresso(null)} title={progresso?.item ? 'Corrigir progresso' : 'Registrar progresso'} onSubmit={salvarProgresso} submitDisabled={busy} submitLabel={busy ? 'Salvando…' : 'Salvar progresso'}>
      <p>{progresso?.atividade.titulo} · {progresso?.atividade.unidade}</p><Select label="Como registrar" value={lancamento.modo} onChange={e => setLancamento({...lancamento,modo:e.target.value})}><option value="INCREMENTO">Adicionar quantidade (fiz mais…)</option><option value="ACUMULADO">Informar acumulado (agora estou em…)</option></Select><Input label={lancamento.modo === 'ACUMULADO' ? 'Total realizado até esta data / página atual' : 'Quantidade feita neste dia'} type="number" required min={lancamento.modo==='ACUMULADO'?'0':progresso?.atividade.fracionavel?'0.0001':'1'} step={progresso?.atividade.fracionavel?'0.0001':'1'} max={progresso?.atividade.total || undefined} value={lancamento.quantidade} onChange={e => setLancamento({...lancamento,quantidade:e.target.value})} /><Input label="Data" type="date" required max={hojeCuiaba()} value={lancamento.data} onChange={e => setLancamento({...lancamento,data:e.target.value})} /><TextArea label="Observação (opcional)" maxLength={4000} value={lancamento.observacao} onChange={e => setLancamento({...lancamento,observacao:e.target.value})} /><Select label="Motivação informada por você (opcional)" value={lancamento.motivacao} onChange={e => setLancamento({...lancamento,motivacao:e.target.value})}><option value="">Não informar</option>{[1,2,3,4,5].map(n => <option key={n}>{n}</option>)}</Select><p className="jornada-muted">Acumulados substituem o saldo naquele ponto da sequência. Correções e registros retroativos recalculam os lançamentos posteriores; acumulados nunca são somados novamente.</p>
    </Modal>
    <Modal open={link !== undefined} onClose={() => !busy && setLink(undefined)} title={link ? 'Editar material' : 'Salvar link e material'} onSubmit={salvarLink} submitDisabled={busy} submitLabel={busy ? 'Salvando…' : 'Salvar link'}><Input label="Título" required maxLength={255} value={linkForm.titulo} onChange={e => setLinkForm({...linkForm,titulo:e.target.value})} /><Input label="URL HTTP ou HTTPS" required type="url" maxLength={2048} value={linkForm.url} onChange={e => setLinkForm({...linkForm,url:e.target.value})} /><TextArea label="Descrição (opcional)" maxLength={4000} value={linkForm.descricao} onChange={e => setLinkForm({...linkForm,descricao:e.target.value})} /><Select label="Atividade (opcional)" value={linkForm.atividadeId} onChange={e => setLinkForm({...linkForm,atividadeId:e.target.value})}><option value="">Recurso geral</option>{atividades.map(a => <option value={a.id} key={a.id}>{a.titulo}</option>)}</Select></Modal>
    <Modal open={estudo !== undefined} onClose={() => !busy && setEstudo(undefined)} title={estudo ? 'Editar sessão de estudo' : 'Registrar sessão de estudo'} onSubmit={salvarEstudo} submitDisabled={busy} submitLabel="Salvar sessão"><Input label="Nome da sessão" required maxLength={255} value={estudoForm.nome} onChange={e => setEstudoForm({...estudoForm,nome:e.target.value})} /><Input label="Data" required type="date" max={hojeCuiaba()} value={estudoForm.data} onChange={e => setEstudoForm({...estudoForm,data:e.target.value})} /><Input label="Duração (minutos)" required type="number" min="0" step="1" value={estudoForm.duracaoMin} onChange={e => setEstudoForm({...estudoForm,duracaoMin:e.target.value})} /><TextArea label="Anotações" maxLength={10000} value={estudoForm.notas} onChange={e => setEstudoForm({...estudoForm,notas:e.target.value})} /></Modal>
  </div></Layout>;
}
function Resumo({ valor, rotulo }: { valor: number; rotulo: string }) { return <div><strong>{valor}</strong><span>{rotulo}</span></div>; }
function Metrica({ titulo, children }: { titulo: string; children: ReactNode }) { return <div><dt>{titulo}</dt><dd>{children}</dd></div>; }
function Metricas({ atividade: a, compacto = false }: { atividade: Atividade; compacto?: boolean }) {
  const i=a.indicadores, dia=i.baseDias==='DIAS_PLANEJADOS'?'dia planejado':'dia corrido';
  const tendencias: Record<string,string>={ SUSPENSA:'Previsões suspensas', CONCLUIDA:'Concluída', NAO_INICIADA:'Início futuro', PRAZO_VENCIDO:'Prazo vencido', SEM_DIAS_DISPONIVEIS:'Sem dias disponíveis', SEM_DADOS:'Sem dados suficientes para estimar', NO_PRAZO:'Ritmo permite cumprir o prazo', FORA_DO_PRAZO:'Ritmo abaixo do necessário' };
  return <><p className="jornada-amount"><strong>{numero(i.realizado)}</strong> / {numero(a.total)} {a.unidade} <span>{i.percentual == null ? 'Cadastro incompleto' : `${numero(i.percentual)}%`}</span></p>{i.percentual != null && <progress max={100} value={i.percentual} aria-label={`${a.titulo}: ${numero(i.percentual)}% concluído`} />}<p className={i.posicao==='ATRASADA'?'jornada-alert':'jornada-position'}>{i.mensagem}</p><dl className="jornada-metrics"><Metrica titulo="Restante">{numero(i.restante)} {a.unidade}</Metrica><Metrica titulo={`Ritmo observado / ${dia}`}>{numero(i.ritmoObservado)} {a.unidade}</Metrica><Metrica titulo={`Necessário / ${dia}`}>{numero(i.necessarioDia)} {a.unidade}</Metrica><Metrica titulo="Previsão de conclusão">{i.conclusaoEstimada ? dataLocal(i.conclusaoEstimada) : i.concluidaEm ? dataLocal(i.concluidaEm) : a.status === 'CONCLUIDA' ? 'Concluída · data desconhecida' : 'Sem dados suficientes para estimar'}</Metrica>{!compacto && <><Metrica titulo="Esperado hoje">{numero(i.esperado)} {a.unidade}</Metrica><Metrica titulo="Projeção no prazo">{numero(i.projetadoNoPrazo)} {a.unidade}</Metrica><Metrica titulo="Necessário / semana">{numero(i.necessarioSemana)} {a.unidade} · prático: {numero(i.praticoSemana)}</Metrica><Metrica titulo="Oportunidades futuras">{i.diasDisponiveis} {i.baseDias==='DIAS_PLANEJADOS'?'dias planejados':'dias corridos'}</Metrica></>}</dl><p><strong>Tendência:</strong> {tendencias[i.tendencia] || i.tendencia}</p><p className="jornada-recommendation">{i.recomendacao}</p>{!compacto && <p className="jornada-muted">Ritmo calculado em {i.diasObservados} {dia === 'dia planejado' ? 'dias planejados' : 'dias corridos'}, incluindo dias sem produção. Recomendações inteiras são arredondadas para cima. Pausas não alteram o prazo.</p>}</>;
}
function Grafico({ atividade: a }: { atividade: Atividade }) {
  const pontos=a.grafico;
  if (pontos.length < 2 || a.total == null || pontos.every(p=>p.planejado==null)) return <p>Complete início, prazo e total para comparar o progresso.</p>;
  const primeiro=Date.parse(pontos[0].data), ultimo=Date.parse(pontos[pontos.length-1].data), amplitude=Math.max(1,ultimo-primeiro);
  const max=Math.max(a.total,...pontos.map(p=>p.realizado),1);
  const x=(d:string)=>45+(Date.parse(d)-primeiro)/amplitude*570, y=(v:number)=>180-v/max*150;
  const real=pontos.filter(p=>p.data<=a.referencia).map(p=>`${x(p.data)},${y(p.realizado)}`).join(' ');
  const planejado=pontos.filter(p=>p.planejado!=null).map(p=>`${x(p.data)},${y(p.planejado!)}`).join(' ');
  return <><svg className="jornada-chart" viewBox="0 0 660 220" role="img" aria-label={`Progresso real ${numero(a.indicadores.realizado)} e esperado ${numero(a.indicadores.esperado)} ${a.unidade}`}><line x1="45" x2="615" y1="180" y2="180" stroke="var(--border)" /><text x="5" y="34">{numero(max)}</text><text x="20" y="184">0</text><polyline points={planejado} fill="none" stroke="var(--color-text-muted)" strokeDasharray="7 5" strokeWidth="2" /><polyline points={real} fill="none" stroke="var(--color-primary)" strokeWidth="3" />{pontos.filter(p=>p.data<=a.referencia).map(p=><circle key={p.data} cx={x(p.data)} cy={y(p.realizado)} r="3" fill="var(--color-primary)"><title>{dataLocal(p.data)}: {numero(p.realizado)}</title></circle>)}<text x="45" y="210">{dataLocal(pontos[0].data)}</text><text x="615" y="210" textAnchor="end">{dataLocal(pontos[pontos.length-1].data)}</text></svg><p className="jornada-muted">━ Real · ┄ Planejado (escala: {a.unidade}). Saldos iniciais não são distribuídos por dias desconhecidos.</p></>;
}
