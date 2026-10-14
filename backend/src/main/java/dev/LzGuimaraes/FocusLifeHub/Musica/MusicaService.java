package dev.LzGuimaraes.FocusLifeHub.Musica;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import dev.LzGuimaraes.FocusLifeHub.Exceptions.BusinessRuleException;
import dev.LzGuimaraes.FocusLifeHub.Exceptions.ResourceNotFoundException;

@Service
@Transactional(readOnly = true)
public class MusicaService {
    private final EstiloMusicalRepository estilos;
    private final MusicaVideoRepository videos;
    public MusicaService(EstiloMusicalRepository estilos, MusicaVideoRepository videos) {
        this.estilos = estilos;
        this.videos = videos;
    }

    public List<MusicaDTO.Estilo> listarEstilos() { return estilos.listarComTotais(); }
    public List<MusicaDTO.Video> listarVideos(Long estiloId) {
        estilo(estiloId);
        return videos.findByEstiloIdOrderByIdDesc(estiloId).stream().map(MusicaDTO.Video::from).toList();
    }
    public MusicaDTO.Video sortear(Long estiloId, Long anteriorId) {
        estilo(estiloId);
        var escolhido = videos.sortear(estiloId, anteriorId == null ? 0L : anteriorId)
                .or(() -> anteriorId == null ? java.util.Optional.empty() : videos.sortear(estiloId, 0L))
                .orElseThrow(() -> new ResourceNotFoundException("Este estilo ainda não possui vídeos. Escolha outro estilo."));
        return MusicaDTO.Video.from(escolhido);
    }

    @Transactional
    public MusicaDTO.Estilo salvarEstilo(Long id, String nome) {
        String limpo = nome.trim().replaceAll("\\s+", " ");
        String chave = Normalizer.normalize(limpo, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
        if (limpo.isBlank() || limpo.length() > 80) throw new BusinessRuleException("Informe um nome de estilo com até 80 caracteres.");
        var model = id == null ? new EstiloMusicalModel() : estiloParaAtualizar(id);
        if (estilos.existsByNomeChaveAndIdNot(chave, id == null ? 0L : id)) throw new BusinessRuleException("Já existe um estilo com esse nome.");
        model.setNome(limpo);
        model.setNomeChave(chave);
        estilos.saveAndFlush(model);
        return estilos.listarComTotais().stream().filter(e -> e.id().equals(model.getId())).findFirst().orElseThrow();
    }

    @Transactional
    public void excluirEstilo(Long id) {
        var model = estiloParaAtualizar(id);
        if (videos.existsByEstiloId(id)) throw new BusinessRuleException("Remova os vídeos deste estilo antes de excluí-lo.");
        estilos.delete(model);
    }

    @Transactional
    public MusicaDTO.Adicao adicionarVideos(Long estiloId, List<String> urls) {
        // Valida o lote inteiro antes de gravar e serializa alterações do mesmo estilo.
        var ids = java.util.stream.IntStream.range(0, urls.size()).mapToObj(i -> {
            try { return YouTubeVideoUrl.id(urls.get(i)); }
            catch (BusinessRuleException ex) { throw new BusinessRuleException("Link " + (i + 1) + ": " + ex.getMessage()); }
        }).toList();
        var model = estiloParaAtualizar(estiloId);
        int adicionados = 0;
        for (String youtubeId : ids) {
            if (videos.existsByEstiloIdAndYoutubeId(estiloId, youtubeId)) continue;
            var video = new MusicaVideoModel();
            video.setEstilo(model);
            preencherVideo(video, youtubeId);
            videos.saveAndFlush(video);
            adicionados++;
        }
        return new MusicaDTO.Adicao(adicionados, urls.size() - adicionados);
    }

    @Transactional
    public MusicaDTO.Video editarVideo(Long id, String url) {
        String youtubeId = YouTubeVideoUrl.id(url);
        var model = video(id);
        estiloParaAtualizar(model.getEstilo().getId());
        if (videos.existsByEstiloIdAndYoutubeIdAndIdNot(model.getEstilo().getId(), youtubeId, id))
            throw new BusinessRuleException("Este vídeo já está cadastrado nesse estilo.");
        preencherVideo(model, youtubeId);
        return MusicaDTO.Video.from(videos.saveAndFlush(model));
    }

    @Transactional
    public void excluirVideo(Long id) {
        var model = video(id);
        estiloParaAtualizar(model.getEstilo().getId());
        videos.delete(model);
    }

    private void preencherVideo(MusicaVideoModel video, String youtubeId) {
        video.setYoutubeId(youtubeId);
        video.setUrl("https://www.youtube.com/watch?v=" + youtubeId);
    }
    private EstiloMusicalModel estilo(Long id) {
        return estilos.findById(id).orElseThrow(() -> new ResourceNotFoundException("Estilo musical não encontrado."));
    }
    private EstiloMusicalModel estiloParaAtualizar(Long id) {
        return estilos.buscarParaAtualizar(id).orElseThrow(() -> new ResourceNotFoundException("Estilo musical não encontrado."));
    }
    private MusicaVideoModel video(Long id) {
        return videos.findById(id).orElseThrow(() -> new ResourceNotFoundException("Vídeo não encontrado."));
    }
}
