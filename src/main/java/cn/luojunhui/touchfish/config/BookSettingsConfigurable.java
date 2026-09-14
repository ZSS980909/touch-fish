package cn.luojunhui.touchfish.config;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.util.ExceptionUtil;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

/**
 * Touch Fish settings page.
 */
public class BookSettingsConfigurable implements Configurable {
    private static final Logger LOGGER = Logger.getInstance(BookSettingsConfigurable.class);
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
            List<String> lines = Files.readAllLines(Paths.get(settings.getBookPath()));
            int totalPage = (lines.size() + settings.getPageSize() - 1) / settings.getPageSize();
            settings.setTotalPage(totalPage);
            settings.setLines(lines);
        } catch (IOException e) {
            throw new ConfigurationException("读取文件失败: " + e.getMessage());
        }
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
