package dev.LzGuimaraes.FocusLifeHub.Musica;

import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import dev.LzGuimaraes.FocusLifeHub.Exceptions.BusinessRuleException;

class YouTubeVideoUrlTest {
    @ParameterizedTest
    @ValueSource(strings = {
        "X4VbdwhkE10", "https://www.youtube.com/watch?v=X4VbdwhkE10",
        "youtu.be/X4VbdwhkE10", "https://youtu.be/X4VbdwhkE10?si=abc",
        "https://m.youtube.com/shorts/X4VbdwhkE10", "https://youtube.com/live/X4VbdwhkE10",
        "https://www.youtube-nocookie.com/embed/X4VbdwhkE10?start=30",
        "https://music.youtube.com/watch?v=X4VbdwhkE10&list=PL_abc"
    })
    void reconheceSomenteIdDoVideo(String url) { assertThat(YouTubeVideoUrl.id(url)).isEqualTo("X4VbdwhkE10"); }

    @ParameterizedTest
    @ValueSource(strings = {
        "", "invalido", "https://example.com/youtube.com/embed/X4VbdwhkE10",
        "https://youtube.com.evil.test/embed/X4VbdwhkE10", "javascript:alert(1)",
        "ftp://youtube.com/watch?v=X4VbdwhkE10", "https://youtube.com:444/watch?v=X4VbdwhkE10",
        "https://youtube.com/watch?v=curto", "https://youtube.com/playlist?list=PL123",
        "https://example.com/watch?v=X4VbdwhkE10", "https://usuario@youtube.com/watch?v=X4VbdwhkE10",
        "https://youtu.be/", "https://youtube.com", "https://youtu.be/X4VbdwhkE10/extra"
    })
    void rejeitaUrlsIndevidas(String url) { assertThatThrownBy(() -> YouTubeVideoUrl.id(url)).isInstanceOf(BusinessRuleException.class); }

    @Test
    void rejeitaNull() { assertThatThrownBy(() -> YouTubeVideoUrl.id(null)).isInstanceOf(BusinessRuleException.class); }
}
