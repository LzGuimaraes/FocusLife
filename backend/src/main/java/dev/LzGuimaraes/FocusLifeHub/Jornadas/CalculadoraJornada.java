package dev.LzGuimaraes.FocusLifeHub.Jornadas;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.stereotype.Component;
import dev.LzGuimaraes.FocusLifeHub.Exceptions.BusinessRuleException;
import static dev.LzGuimaraes.FocusLifeHub.Jornadas.JornadasDTO.*;

/** Snapshots ao fim do dia local: hoje é observado, amanhã é a primeira oportunidade futura. */
@Component
public class CalculadoraJornada {
    public record Saldo(BigDecimal quantidade, LocalDate concluidaEm) {}
    public Saldo saldo(Atividade a,List<Progresso> historico,LocalDate ate) {
        BigDecimal valor = a.getSaldoData()!=null && ate.isBefore(a.getSaldoData()) ? BigDecimal.ZERO : a.getSaldoInicial();
        LocalDate fim = null; // Saldo inicial completo não informa o dia real de conclusão.
        for (Progresso p : historico.stream().sorted(Comparator.comparing(Progresso::getData).thenComparing(p -> p.getId()==null ? Long.MAX_VALUE : p.getId())).toList()) {
            if (p.getData().isAfter(ate)) continue;
            if (a.getSaldoData()==null || !p.getData().isAfter(a.getSaldoData())) erro("Lançamentos devem ser posteriores à data do saldo inicial (saldo ao fim do dia).");
            if (a.getInicio()!=null && p.getData().isBefore(a.getInicio())) erro("O progresso não pode ser anterior ao início.");
            BigDecimal novo = p.getModo()==Progresso.Modo.ACUMULADO ? p.getQuantidade() : valor.add(p.getQuantidade());
            if (novo.compareTo(valor)<0) erro("O acumulado não pode reduzir o saldo anterior. Corrija os lançamentos anteriores.");
            if (a.getTotal()!=null && novo.compareTo(a.getTotal())>0) erro("O progresso ultrapassa a quantidade total.");
            if (!a.isFracionavel() && novo.stripTrailingZeros().scale()>0) erro("Esta unidade exige quantidades inteiras.");
            boolean atingiuTotal=a.getTotal()!=null && valor.compareTo(a.getTotal())<0 && novo.compareTo(a.getTotal())==0;
            valor=novo;
            if (atingiuTotal && fim==null) fim=p.getData();
        }
        return new Saldo(valor,fim);
    }
    public long dias(Atividade a,LocalDate de,LocalDate ate) {
        if (de==null || ate==null || ate.isBefore(de)) return 0;
        long n=ChronoUnit.DAYS.between(de,ate)+1;
        if (a.getDiasSemana().isEmpty()) return n;
        long total=(n/7)*a.getDiasSemana().size();
        for (int i=0;i<n%7;i++) if (a.getDiasSemana().contains(de.plusDays(n-n%7+i).getDayOfWeek())) total++;
        return total;
    }
    public Double esperado(Atividade a,LocalDate data) {
        if (a.getTotal()==null || a.getInicio()==null || a.getPrazo()==null) return null;
        long totalDias=dias(a,a.getInicio(),a.getPrazo());
        return totalDias==0 ? null : a.getTotal().doubleValue()*dias(a,a.getInicio(),data.isAfter(a.getPrazo())?a.getPrazo():data)/totalDias;
    }
    public Indicadores calcular(Atividade a,List<Progresso> historico,LocalDate hoje) {
        Saldo saldo=saldo(a,historico,hoje);
        double feito=saldo.quantidade().doubleValue();
        Double total=a.getTotal()==null?null:a.getTotal().doubleValue();
        Double restante=total==null?null:Math.max(0,total-feito);
        Double percentual=total==null?null:100*feito/total;
        Double esperado=esperado(a,hoje), diferenca=esperado==null?null:feito-esperado;
        boolean ativa=a.getStatus()!=StatusAtividade.PAUSADA && a.getStatus()!=StatusAtividade.CANCELADA;
        boolean concluida=restante!=null && restante==0;
        LocalDate inicioObservado=a.getSaldoData()==null?null:a.getSaldoData().plusDays(1);
        long observados=dias(a,inicioObservado,hoje);
        boolean temHistorico=historico.stream().anyMatch(p -> !p.getData().isAfter(hoje));
        double produzido=feito-a.getSaldoInicial().doubleValue();
        Double ritmo=temHistorico && observados>0 && produzido>0 ? produzido/observados : null;
        LocalDate futuro=a.getInicio()!=null && a.getInicio().isAfter(hoje)?a.getInicio():hoje.plusDays(1);
        long disponiveis=dias(a,futuro,a.getPrazo());
        Double necessario=ativa && !concluida && restante!=null && disponiveis>0?restante/disponiveis:null;
        Double semana=necessario==null?null:necessario*(a.getDiasSemana().isEmpty()?7:a.getDiasSemana().size());
        Double projetado=ativa && ritmo!=null && a.getPrazo()!=null && !a.getPrazo().isBefore(hoje)?feito+ritmo*disponiveis:null;
        LocalDate estimada=null;
        if (ativa && !concluida && ritmo!=null && restante!=null) {
            double necessarios=Math.ceil(restante/ritmo-1e-10);
            // Limite explícito: não produzir datas absurdas para ritmos próximos de zero.
            if (necessarios<=36525) {
                long alvo=(long)necessarios;
                long baixo=0,alto=51136;
                while(baixo<alto) { long meio=(baixo+alto)/2; if(dias(a,futuro,futuro.plusDays(meio))>=alvo) alto=meio; else baixo=meio+1; }
                estimada=futuro.plusDays(baixo);
            }
        }
        String posicao=esperado==null?"SEM_PLANEJAMENTO":diferenca>0.0001?"ADIANTADA":diferenca< -0.0001?"ATRASADA":"NO_PLANEJADO";
        String situacao=a.getPrazo()==null?"SEM_PRAZO":concluida?"CONCLUIDA":a.getPrazo().isBefore(hoje)?"VENCIDA":a.getPrazo().equals(hoje)?"HOJE":"NO_PRAZO";
        String tendencia=!ativa?"SUSPENSA":concluida?"CONCLUIDA":a.getInicio()!=null && hoje.isBefore(a.getInicio())?"NAO_INICIADA":
            a.getPrazo()!=null && a.getPrazo().isBefore(hoje)?"PRAZO_VENCIDO":disponiveis==0 && a.getPrazo()!=null?"SEM_DIAS_DISPONIVEIS":
            projetado==null || total==null?"SEM_DADOS":projetado+0.0001>=total?"NO_PRAZO":"FORA_DO_PRAZO";
        String unidade=a.getUnidade()==null?"unidades":a.getUnidade();
        String mensagem=diferenca==null?"Complete início, prazo e quantidade para acompanhar o planejamento.":
            Math.abs(diferenca)<0.0001?"Você está dentro do planejado.":"Você está "+numero(Math.abs(diferenca))+" "+unidade+(diferenca<0?" abaixo":" acima")+" do planejado.";
        String recomendacao;
        if (!ativa) recomendacao="Atividade "+(a.getStatus()==StatusAtividade.PAUSADA?"pausada":"cancelada")+". Previsões suspensas; o prazo e os dias de pausa permanecem no planejamento.";
        else if (concluida) recomendacao=saldo.concluidaEm()==null?"Concluída; data de conclusão desconhecida no saldo inicial.":
            "Concluída em "+saldo.concluidaEm().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))+(a.getPrazo()==null?".":saldo.concluidaEm().isAfter(a.getPrazo())?", depois do prazo.":", dentro do prazo.");
        else if ("HOJE".equals(situacao)) recomendacao=restante==null?"O prazo é hoje. Complete a quantidade desejada para calcular o restante.":"O prazo é hoje. Restam "+numero(restante)+" "+unidade+"; registre o que realizou hoje. Não há dias futuros até o prazo.";
        else if ("VENCIDA".equals(situacao)) recomendacao="Prazo vencido. Revise o planejamento; o prazo não foi alterado automaticamente.";
        else if (a.getPrazo()!=null && disponiveis==0) recomendacao="Não há dias planejados disponíveis até o prazo. Revise os dias ou o prazo.";
        else if (ritmo==null) recomendacao="Sem dados suficientes para estimar. Registre produção após a data do saldo inicial."+(necessario==null?"":" Para terminar no prazo, faça aproximadamente "+numero(a.isFracionavel()?necessario:Math.ceil(necessario))+" "+unidade+" por "+(a.getDiasSemana().isEmpty()?"dia":"dia planejado")+".");
        else if (necessario!=null) recomendacao=("ADIANTADA".equals(posicao) && ritmo>necessario?"Você está adiantado e pode reduzir para ":"Para terminar no prazo, faça aproximadamente ")+numero(a.isFracionavel()?necessario:Math.ceil(necessario))+" "+unidade+" por "+(a.getDiasSemana().isEmpty()?"dia":"dia planejado")+".";
        else recomendacao="Complete o cadastro para calcular o esforço necessário.";
        return new Indicadores(feito,percentual,restante,esperado,diferenca,ritmo,projetado,estimada,saldo.concluidaEm(),
            necessario,semana,necessario==null?null:a.isFracionavel()?necessario:Math.ceil(necessario),semana==null?null:a.isFracionavel()?semana:Math.ceil(semana),
            observados,disponiveis,a.getDiasSemana().isEmpty()?"DIAS_CORRIDOS":"DIAS_PLANEJADOS",posicao,tendencia,situacao,mensagem,recomendacao);
    }
    public List<Ponto> grafico(Atividade a,List<Progresso> historico,LocalDate hoje) {
        TreeSet<LocalDate> datas=new TreeSet<>();
        if(a.getInicio()!=null) datas.add(a.getInicio());
        if(a.getSaldoData()!=null) datas.add(a.getSaldoData());
        if(a.getPrazo()!=null) datas.add(a.getPrazo());
        datas.add(hoje);
        historico.forEach(p -> datas.add(p.getData()));
        List<LocalDate> todas=new ArrayList<>(datas);
        List<Ponto> pontos=new ArrayList<>();
        int passo=Math.max(1,(int)Math.ceil(todas.size()/200.0));
        for(int i=0;i<todas.size();i++) if(i%passo==0 || i==todas.size()-1) {
            LocalDate d=todas.get(i);
            if(a.getSaldoData()!=null && d.isBefore(a.getSaldoData()) && a.getSaldoInicial().signum()>0) continue;
            pontos.add(new Ponto(d,saldo(a,historico,d.isAfter(hoje)?hoje:d).quantidade().doubleValue(),esperado(a,d)));
        }
        return pontos;
    }
    private static String numero(double n) { return String.format(Locale.forLanguageTag("pt-BR"),"%.2f",n).replaceAll(",00$",""); }
    private static void erro(String mensagem) { throw new BusinessRuleException(mensagem); }
}
