import { useEffect, useState } from 'react';
import api from '../api/api';
import type { Atividade } from '../types/jornadas';

interface Props {
  atividade: Atividade;
  readonly?: boolean;
  busy: boolean;
  executar: (acao: () => Promise<unknown>, sucesso?: string) => Promise<void>;
}

/** O backend preserva e recupera sessões abertas; o contador local apenas atualiza a exibição. */
export default function EstudoTimer({ atividade, readonly = false, busy, executar }: Props) {
  const [agora, setAgora] = useState(Date.now());
  const abertas = atividade.sessoes.filter(sessao => !sessao.fim);

  useEffect(() => {
    if (!abertas.length) return;
    const timer = setInterval(() => setAgora(Date.now()), 1000);
    return () => clearInterval(timer);
  }, [abertas.length]);

  // O deslocamento de America/Cuiaba vem explícito na API, independente do fuso do navegador.
  const segundos = atividade.totalSegundos + abertas.reduce((soma, sessao) =>
    soma + Math.max(0, Math.floor((agora - Date.parse(sessao.inicio)) / 1000)), 0);
  const duracao = [Math.floor(segundos / 3600), Math.floor(segundos / 60) % 60, segundos % 60]
    .map(n => String(n).padStart(2, '0')).join(':');

  return (
    <div className="jornada-timer">
      <strong>⏱ {duracao}</strong>
      {!readonly && (abertas.length ? abertas.map(sessao => (
        <button key={sessao.id} className="jornada-button" disabled={busy}
          onClick={() => void executar(() => api.put(`/jornadas/${atividade.id}/sessoes/${sessao.id}/pausar`), 'Cronômetro pausado')}>
          Pausar sessão #{sessao.id}
        </button>
      )) : (
        <button className="jornada-button primary" disabled={busy || ['PAUSADA', 'CANCELADA'].includes(atividade.status)}
          onClick={() => void executar(() => api.post(`/jornadas/${atividade.id}/sessoes`), 'Cronômetro iniciado')}>
          Iniciar cronômetro
        </button>
      ))}
    </div>
  );
}
