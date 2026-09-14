package cn.luojunhui.touchfish.config;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.util.ExceptionUtil;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

/**
 * Touch Fish settings page.
 */
public class BookSettingsConfigurable implements Configurable {
    private static final Logger LOGGER = Logger.getInstance(BookSettingsConfigurable.class);
    private static final Charset GB18030 = Charset.forName("GB18030");

    private BookSettingsComponent bookSettingsComponent;

    @Override
    public @NlsContexts.ConfigurableName String getDisplayName() {
        return "Touch Fish";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        if (bookSettingsComponent == null) {
            bookSettingsComponent = new BookSettingsComponent();
            try {
                bookSettingsComponent.init();
            } catch (Exception e) {
                LOGGER.error("Touch Fish settings panel initialization failed:\n" + ExceptionUtil.currentStackTrace(), e);
            }
        }
        return bookSettingsComponent.getPanel();
    }

    @Override
    public boolean isModified() {
        if (bookSettingsComponent == null) {
            return false;
        }
        BookSettingsState settings = BookSettingsState.getInstance();
        return !Objects.equals(settings.getBookPath().trim(), bookSettingsComponent.getBookPath())
                || settings.getPage() != bookSettingsComponent.getPage()
                || settings.getPageSize() != bookSettingsComponent.getPageSize();
    }

    @Override
    public void apply() throws ConfigurationException {
        if (bookSettingsComponent == null) {
            return;
        }

        BookSettingsState settings = BookSettingsState.getInstance();
        settings.setBookPath(bookSettingsComponent.getBookPath());
        settings.setPage(bookSettingsComponent.getPage());
        settings.setPageSize(bookSettingsComponent.getPageSize());

        if (settings.getBookPath().isBlank()) {
            settings.setLines(List.of());
            settings.setTotalPage(0);
            return;
        }

        try {
            List<String> lines = readTextLines(Paths.get(settings.getBookPath()));
            int totalPage = (lines.size() + settings.getPageSize() - 1) / settings.getPageSize();
            settings.setTotalPage(totalPage);
            settings.setLines(lines);
        } catch (IOException e) {
            throw new ConfigurationException("读取文件失败: " + e.getMessage());
        }
    }

    /**
     * Reads common Chinese TXT encodings automatically.
     * <p>
     * Priority: UTF-8 BOM / UTF-16 BOM -> strict UTF-8 -> GB18030 fallback.
     * GB18030 is backward-compatible with the vast majority of GBK/GB2312 novel files.
     */
    private List<String> readTextLines(Path path) throws IOException {
        byte[] bytes = Files.readAllBytes(path);
        if (bytes.length == 0) {
            return List.of();
        }

        String text;
        if (hasPrefix(bytes, 0xEF, 0xBB, 0xBF)) {
            text = new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);
        } else if (hasPrefix(bytes, 0xFF, 0xFE)) {
            text = new String(bytes, 2, bytes.length - 2, StandardCharsets.UTF_16LE);
        } else if (hasPrefix(bytes, 0xFE, 0xFF)) {
            text = new String(bytes, 2, bytes.length - 2, StandardCharsets.UTF_16BE);
        } else {
            text = decodeUtf8OrGb18030(bytes);
        }

        return text.lines().toList();
    }

    private String decodeUtf8OrGb18030(byte[] bytes) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException ignored) {
            LOGGER.info("TXT is not valid UTF-8, falling back to GB18030");
            return new String(bytes, GB18030);
        }
    }

    private boolean hasPrefix(byte[] bytes, int... prefix) {
        if (bytes.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if ((bytes[i] & 0xFF) != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void reset() {
        if (bookSettingsComponent == null) {
            return;
        }
        BookSettingsState settings = BookSettingsState.getInstance();
        bookSettingsComponent.setBookPath(settings.getBookPath() == null ? "" : settings.getBookPath());
        bookSettingsComponent.setPage(settings.getPage() == null ? 1 : settings.getPage());
        bookSettingsComponent.setPageSize(settings.getPageSize() == null ? 5 : settings.getPageSize());
    }

    @Override
    public void disposeUIResources() {
        bookSettingsComponent = null;
    }
}
