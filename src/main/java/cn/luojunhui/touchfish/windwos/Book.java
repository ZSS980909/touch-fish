package cn.luojunhui.touchfish.windwos;

import cn.luojunhui.touchfish.config.BookSettingsState;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Touch Fish reader tool window.
 *
 * Uses plain Swing instead of IntelliJ GUI Designer .form instrumentation so it
 * works reliably on modern IntelliJ Platform versions.
 */
public class Book {
    private static final Logger LOGGER = Logger.getInstance(Book.class);

    private static final int PREV = 0;
    private static final int NEXT = 1;
    private static final int CURRENT = 2;

    private final JPanel book;
    private final JTextPane text;

    public Book(ToolWindow toolWindow) {
        Color panelBackground = UIUtil.getPanelBackground();
        EditorColorsScheme editorScheme = EditorColorsManager.getInstance().getGlobalScheme();

        text = new JTextPane();
        text.setEditable(false);
        text.setOpaque(true);
        text.setBackground(panelBackground);
        text.setForeground(UIUtil.getLabelForeground());
        text.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        text.setFont(new Font(
                editorScheme.getEditorFontName(),
                Font.PLAIN,
                editorScheme.getEditorFontSize()
        ));

        JBScrollPane scrollPane = new JBScrollPane(
                text,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        );
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(true);
        scrollPane.getViewport().setBackground(panelBackground);
        scrollPane.getVerticalScrollBar().setUnitIncrement(12);

        book = new JPanel(new BorderLayout());
        book.setBackground(panelBackground);
        book.add(scrollPane, BorderLayout.CENTER);

        installPageActions();
        init();
    }

    private void init() {
        BookSettingsState settings = BookSettingsState.getInstance();
        if (settings == null) {
            showMessage("请先到插件面板设置阅读信息。");
            return;
        }

        if (StringUtil.isNotEmpty(settings.getBookPath())) {
            if (settings.getLines() == null || settings.getLines().isEmpty()) {
                setText("已设置文本文件，但当前没有已加载的内容。\n请到 Settings → Tools → Touch Fish 点击 Apply/OK 重新加载。");
            } else {
                readText(CURRENT);
            }
        } else {
            setText("没有文本文件路径...\n请到 Settings → Tools → Touch Fish 选择 txt 文件。");
        }
    }

    private void installPageActions() {
        InputMap inputMap = text.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap actionMap = text.getActionMap();

        inputMap.put(KeyStroke.getKeyStroke("UP"), "touchFishPrev");
        inputMap.put(KeyStroke.getKeyStroke("DOWN"), "touchFishNext");

        actionMap.put("touchFishPrev", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                readText(PREV);
            }
        });

        actionMap.put("touchFishNext", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                readText(NEXT);
            }
        });
    }

    private void readText(int op) {
        BookSettingsState settings = BookSettingsState.getInstance();
        if (settings == null) {
            showMessage("请先到插件面板设置阅读信息。");
            return;
        }

        List<String> allLines = settings.getLines();
        if (allLines == null || allLines.isEmpty()) {
            setText("当前没有可显示的文本内容。\n请到 Settings → Tools → Touch Fish 重新选择文件并点击 Apply/OK。");
            return;
        }

        int pageSize = settings.getPageSize() == null || settings.getPageSize() < 1
                ? 3 : settings.getPageSize();
        int totalPage = Math.max(1, (allLines.size() + pageSize - 1) / pageSize);
        settings.setTotalPage(totalPage);

        int curPage = settings.getPage() == null ? 1 : settings.getPage();
        curPage = Math.max(1, Math.min(curPage, totalPage));

        int targetPage = curPage;
        if (op == PREV) {
            targetPage = Math.max(1, curPage - 1);
            if (targetPage == curPage) {
                showMessage("不能再往前翻页了...");
            }
        } else if (op == NEXT) {
            targetPage = Math.min(totalPage, curPage + 1);
            if (targetPage == curPage) {
                showMessage("不能再往后翻页了...");
            }
        }

        settings.setPage(targetPage);
        setText(readFromPage(allLines, targetPage, pageSize));
    }

    private List<String> readFromPage(List<String> list, int page, int pageSize) {
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        return list.stream()
                .skip((long) (page - 1) * pageSize)
                .limit(pageSize)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private void setText(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            text.setText("");
            return;
        }
        text.setText(String.join("\n", lines));
        text.setCaretPosition(0);
    }

    private void setText(String value) {
        text.setText(value == null ? "" : value);
        text.setCaretPosition(0);
    }

    private void showMessage(String info) {
        LOGGER.info(info);
        Notifications.Bus.notify(new Notification("", "Touch Fish", info, NotificationType.INFORMATION));
    }

    public JComponent getContent() {
        return book;
    }
}
