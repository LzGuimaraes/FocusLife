package dev.LzGuimaraes.FocusLifeHub.Jornadas;
import java.math.BigDecimal;
import java.net.URI;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import dev.LzGuimaraes.FocusLifeHub.Exceptions.*;
import dev.LzGuimaraes.FocusLifeHub.Materia.*;
import dev.LzGuimaraes.FocusLifeHub.Estudos.*;
import dev.LzGuimaraes.FocusLifeHub.User.UserRepository;
import static dev.LzGuimaraes.FocusLifeHub.Jornadas.JornadasDTO.*;
@Service @Transactional
public class JornadasService {
    private final AtividadeRepository atividades;
    private final ProgressoRepository progressos;
    private final MaterialRepository materiais;
    private final SessaoEstudoRepository sessoes;
    private final EstudosRepository estudos;
    private final UserRepository usuarios;
    private final CalculadoraJornada calculadora;
    private final Clock clock;
    public JornadasService(AtividadeRepository atividades,ProgressoRepository progressos,MaterialRepository materiais,
        SessaoEstudoRepository sessoes,EstudosRepository estudos,UserRepository usuarios,CalculadoraJornada calculadora,Clock clock) {
        this.atividades=atividades;this.progressos=progressos;this.materiais=materiais;this.sessoes=sessoes;this.estudos=estudos;
        this.usuarios=usuarios;this.calculadora=calculadora;this.clock=clock.withZone(ZoneId.of("America/Cuiaba"));
    }
    private LocalDate hoje() { return LocalDate.now(clock); }
    @Transactional(readOnly=true) public List<Resposta> listar(Long user) { return atividades.findByUserIdOrderByIdDesc(user).stream().map(a->resposta(a,false)).toList(); }
    @Transactional(readOnly=true) public Resposta buscar(Long user,Long id) { return resposta(encontrar(user,id,false),true); }
    private Atividade encontrar(Long user,Long id,boolean lock) { return (lock?atividades.bloquear(id,user):atividades.findByIdAndUserId(id,user)).orElseThrow(()->new ResourceNotFoundException("Atividade não encontrada")); }
    public Resposta salvar(Long user,Long id,Pedido p) {
        Atividade a=id==null?new Atividade():encontrar(user,id,true);
        if(id!=null) versao(a,p.version());
        else if(!usuarios.existsById(user)) throw new ResourceNotFoundException("Usuário não encontrado");
        a.setUserId(user);a.setTitulo(p.titulo().trim());a.setDescricao(p.descricao());a.setInicio(p.inicio());a.setPrazo(p.prazo());
        a.setUnidade(vazio(p.unidade()));a.setFracionavel(p.fracionavel());a.setTotal(p.total());a.setSaldoInicial(p.saldoInicial());
        a.setSaldoData(p.saldoData()!=null?p.saldoData():p.saldoInicial().signum()==0 && p.inicio()!=null?p.inicio().minusDays(1):hoje());
        a.setStatus(p.status());a.setPrioridade(vazio(p.prioridade()));a.setHorario(p.horario());a.setAgrupamento(vazio(p.agrupamento()));
        a.setDiasSemana(p.diasSemana()==null?new HashSet<>():new HashSet<>(p.diasSemana()));
        validar(a);
        List<Progresso> historico=id==null?List.of():progressos.findByAtividadeIdOrderByDataAscIdAsc(id);
        coerencia(a,historico,false);
        a.setRevisao(a.getRevisao()+1);atividades.saveAndFlush(a);return resposta(a,true);
    }
    private void validar(Atividade a) {
        if(a.getInicio()!=null && a.getPrazo()!=null && a.getPrazo().isBefore(a.getInicio())) erro("O prazo deve ser igual ou posterior ao início.");
        if(a.getInicio()!=null && a.getPrazo()!=null && java.time.temporal.ChronoUnit.DAYS.between(a.getInicio(),a.getPrazo())>36525) erro("O período máximo é de 100 anos.");
        if(a.getSaldoData().isAfter(hoje()) && a.getSaldoInicial().signum()>0) erro("O saldo realizado não pode ter data futura.");
        if(a.getTotal()!=null && a.getTotal().signum()<=0) erro("O total deve ser maior que zero.");
        if(a.getTotal()!=null && a.getUnidade()==null) erro("Informe a unidade de medida.");
        if(a.getTotal()!=null && a.getSaldoInicial().compareTo(a.getTotal())>0) erro("O saldo inicial ultrapassa o total.");
        if(!a.isFracionavel() && (a.getSaldoInicial().stripTrailingZeros().scale()>0 || a.getTotal()!=null && a.getTotal().stripTrailingZeros().scale()>0)) erro("Esta unidade exige quantidades inteiras.");
        if(a.getInicio()!=null && a.getSaldoInicial().signum()>0 && a.getSaldoData().isBefore(a.getInicio())) erro("O saldo realizado deve ter referência a partir do início.");
        if(a.getPrioridade()!=null && !Set.of("baixa","media","alta").contains(a.getPrioridade())) erro("Prioridade inválida.");
        if(a.getDiasSemana().contains(null)) erro("Dia da semana inválido.");
    }
    private void coerencia(Atividade a,List<Progresso> historico,boolean automatico) {
        var saldo=calculadora.saldo(a,historico,hoje()).quantidade();
        boolean completa=a.getTotal()!=null && saldo.compareTo(a.getTotal())==0;
        if(automatico) {
            if(completa) a.setStatus(StatusAtividade.CONCLUIDA);
            else if(a.getStatus()==StatusAtividade.CONCLUIDA || a.getStatus()==StatusAtividade.NAO_INICIADA && saldo.signum()>0) a.setStatus(saldo.signum()>0?StatusAtividade.EM_ANDAMENTO:StatusAtividade.NAO_INICIADA);
        }
        if(a.getStatus()==StatusAtividade.CONCLUIDA && !completa) erro("Para concluir, registre toda a quantidade desejada.");
        if(completa && a.getStatus()!=StatusAtividade.CONCLUIDA) erro("Uma atividade com progresso completo deve estar concluída.");
        if(a.getStatus()==StatusAtividade.NAO_INICIADA && saldo.signum()>0) erro("Uma atividade com progresso deve estar em andamento.");
    }
    public Resposta lancar(Long user,Long id,Long progressoId,Lancamento p) {
        Atividade a=encontrar(user,id,true);
        if(progressoId==null && progressos.findByAtividadeIdAndRequestId(id,p.requestId()).isPresent()) return resposta(a,true);
        versao(a,p.version());
        if(a.getStatus()==StatusAtividade.CANCELADA) erro("Reative a atividade antes de registrar progresso.");
        if(a.getTotal()==null || a.getUnidade()==null) erro("Complete a quantidade total e a unidade antes de registrar progresso.");
        if(p.data().isAfter(hoje())) erro("Não é permitido registrar progresso futuro.");
        if(p.modo()==Progresso.Modo.INCREMENTO && p.quantidade().signum()<=0) erro("O incremento deve ser maior que zero.");
        Progresso item=progressoId==null?new Progresso():progressos.findById(progressoId).filter(x->x.getAtividadeId().equals(id)).orElseThrow(()->new ResourceNotFoundException("Progresso não encontrado"));
        item.setAtividadeId(id);if(progressoId==null) item.setRequestId(p.requestId());item.setData(p.data());item.setModo(p.modo());item.setQuantidade(p.quantidade());item.setObservacao(p.observacao());item.setMotivacao(p.motivacao());
        progressos.saveAndFlush(item);
        coerencia(a,progressos.findByAtividadeIdOrderByDataAscIdAsc(id),true);tocar(a);return resposta(a,true);
    }
    public Resposta excluirProgresso(Long user,Long id,Long progressoId,long version) {
        Atividade a=encontrar(user,id,true);versao(a,version);
        Progresso item=progressos.findById(progressoId).filter(x->x.getAtividadeId().equals(id)).orElseThrow(()->new ResourceNotFoundException("Progresso não encontrado"));
        progressos.delete(item);progressos.flush();coerencia(a,progressos.findByAtividadeIdOrderByDataAscIdAsc(id),true);tocar(a);return resposta(a,true);
    }
    public void excluir(Long user,Long id,long version) {
        Atividade a=encontrar(user,id,true);versao(a,version);
        if(sessoes.findByAtividadeId(id).stream().anyMatch(s->s.getFim()==null)) erro("Pause o cronômetro antes de excluir a atividade.");
        // Materiais passam a recursos gerais; sessões e anotações são removidas apenas por exclusão explícita da atividade.
        materiais.findByAtividadeId(id).forEach(m->m.setAtividadeId(null));materiais.flush();
        progressos.deleteByAtividadeId(id);sessoes.deleteByAtividadeId(id);estudos.deleteByAtividadeId(id);atividades.delete(a);
    }
    private Resposta resposta(Atividade a,boolean detalhe) {
        var historico=progressos.findByAtividadeIdOrderByDataAscIdAsc(a.getId());var ss=sessoes.findByAtividadeId(a.getId());var notas=estudos.findByAtividadeId(a.getId());
        long total=ss.stream().mapToLong(SessaoEstudo::getDuracaoSegundos).sum();
        return new Resposta(a.getId(),a.getVersion(),a.getTitulo(),a.getDescricao(),a.getInicio(),a.getPrazo(),a.getUnidade(),a.isFracionavel(),a.getTotal(),a.getSaldoInicial(),a.getSaldoData(),a.getStatus(),a.getPrioridade(),a.getHorario(),Set.copyOf(a.getDiasSemana()),a.getAgrupamento(),a.getOrigem(),a.getPercentualLegado(),hoje(),calculadora.calcular(a,historico,hoje()),detalhe?historico:List.of(),detalhe?calculadora.grafico(a,historico,hoje()):List.of(),detalhe?materiais.findByAtividadeId(a.getId()):List.of(),total,
            detalhe?ss.stream().map(s->new Sessao(s.getId(),s.getInicio().atZone(clock.getZone()).toOffsetDateTime(),s.getFim()==null?null:s.getFim().atZone(clock.getZone()).toOffsetDateTime(),s.getDuracaoSegundos())).toList():List.of(),
            detalhe?notas.stream().map(e->new Anotacao(e.getId(),e.getNome(),e.getData(),e.getDuracao_min(),e.getNotas())).toList():List.of());
    }
    @Transactional(readOnly=true) public List<Material> links(Long user) { return materiais.findByUserIdOrderByIdDesc(user); }
    public Material salvarLink(Long user,Long id,LinkPedido p) {
        if(p.atividadeId()!=null) encontrar(user,p.atividadeId(),true);
        String url=p.url().trim();
        try { URI u=URI.create(url);if(!("http".equalsIgnoreCase(u.getScheme()) || "https".equalsIgnoreCase(u.getScheme())) || u.getHost()==null || u.getUserInfo()!=null) erro("Use uma URL HTTP ou HTTPS válida, sem credenciais."); }
        catch(IllegalArgumentException ex) { erro("URL inválida."); }
        Material m=id==null?new Material():materiais.findByIdAndUserId(id,user).orElseThrow(()->new ResourceNotFoundException("Material não encontrado"));
        if(id!=null && (p.version()==null || p.version()!=m.getVersion())) conflito();
        m.setUserId(user);m.setAtividadeId(p.atividadeId());m.setTitulo(p.titulo().trim());m.setUrl(url);m.setDescricao(p.descricao());return materiais.saveAndFlush(m);
    }
    public void excluirLink(Long user,Long id,long version) {
        Material m=materiais.findByIdAndUserId(id,user).orElseThrow(()->new ResourceNotFoundException("Material não encontrado"));
        if(m.getVersion()!=version) conflito();materiais.delete(m);
    }
    public Resposta iniciar(Long user,Long id) {
        Atividade a=encontrar(user,id,true);
        if(a.getStatus()==StatusAtividade.CANCELADA || a.getStatus()==StatusAtividade.PAUSADA) erro("Reative a atividade antes de iniciar o cronômetro.");
        if(sessoes.findByAtividadeId(id).stream().noneMatch(s->s.getFim()==null)) {
            SessaoEstudo s=new SessaoEstudo();s.setAtividadeId(id);s.setInicio(LocalDateTime.now(clock));sessoes.saveAndFlush(s);
        }
        return resposta(a,true);
    }
    public Resposta pausar(Long user,Long id,Long sessao) {
        Atividade a=encontrar(user,id,true);
        SessaoEstudo s=sessoes.findById(sessao).filter(x->id.equals(x.getAtividadeId())).orElseThrow(()->new ResourceNotFoundException("Sessão não encontrada"));
        if(s.getFim()==null) {s.setFim(LocalDateTime.now(clock));s.setDuracaoSegundos(Math.max(0,Duration.between(s.getInicio().atZone(clock.getZone()),s.getFim().atZone(clock.getZone())).getSeconds()));sessoes.saveAndFlush(s);}
        return resposta(a,true);
    }
    public Resposta anotar(Long user,Long id,Long notaId,EstudoPedido p) {
        Atividade a=encontrar(user,id,true);
        if(p.data().isAfter(hoje())) erro("A sessão não pode ter data futura.");
        EstudosModel e=notaId==null?new EstudosModel():estudos.findById(notaId).filter(x->id.equals(x.getAtividadeId())).orElseThrow(()->new ResourceNotFoundException("Estudo não encontrado"));
        e.setAtividadeId(id);e.setNome(p.nome().trim());e.setData(p.data());e.setDuracao_min(p.duracaoMin());e.setNotas(p.notas());estudos.saveAndFlush(e);return resposta(a,true);
    }
    public Resposta excluirEstudo(Long user,Long id,Long notaId) {
        Atividade a=encontrar(user,id,true);EstudosModel e=estudos.findById(notaId).filter(x->id.equals(x.getAtividadeId())).orElseThrow(()->new ResourceNotFoundException("Estudo não encontrado"));
        estudos.delete(e);estudos.flush();return resposta(a,true);
    }
    private void tocar(Atividade a) { a.setRevisao(a.getRevisao()+1);atividades.saveAndFlush(a); }
    private static String vazio(String s) { return s==null || s.isBlank()?null:s.trim(); }
    private static void versao(Atividade a,Long version) { if(version==null || a.getVersion()!=version) conflito(); }
    private static void conflito() { throw new ResponseStatusException(HttpStatus.CONFLICT,"Os dados mudaram. Atualize a página antes de salvar."); }
    private static void erro(String s) { throw new BusinessRuleException(s); }
}
