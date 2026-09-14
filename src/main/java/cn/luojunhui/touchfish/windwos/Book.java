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
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
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
    private final JBLabel pageInfoLabel;
    private final JBTextField jumpPageField;

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

        pageInfoLabel = new JBLabel("第 0 / 0 页");
        jumpPageField = new JBTextField();
        jumpPageField.setColumns(5);
        jumpPageField.setToolTipText("输入页码后按 Enter 跳转");

        JButton jumpButton = new JButton("跳转");
        jumpButton.addActionListener(e -> jumpToPage());
        jumpPageField.addActionListener(e -> jumpToPage());

        JPanel pageBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 3));
        pageBar.setOpaque(true);
        pageBar.setBackground(panelBackground);
        pageBar.setBorder(JBUI.Borders.empty(2, 4));
        pageBar.add(pageInfoLabel);
        pageBar.add(new JBLabel("跳到"));
        pageBar.add(jumpPageField);
        pageBar.add(new JBLabel("页"));
        pageBar.add(jumpButton);

        book = new JPanel(new BorderLayout());
        book.setBackground(panelBackground);
        book.add(scrollPane, BorderLayout.CENTER);
        book.add(pageBar, BorderLayout.SOUTH);

        installPageActions();
        init();
    }

    private void init() {
        BookSettingsState settings = BookSettingsState.getInstance();
        if (settings == null) {
            showMessage("请先到插件面板设置阅读信息。");
            updatePageInfo(0, 0);
            return;
        }

        if (StringUtil.isNotEmpty(settings.getBookPath())) {
            if (settings.getLines() == null || settings.getLines().isEmpty()) {
                setText("已设置文本文件，但当前没有已加载的内容。\n请到 Settings → Tools → Touch Fish 点击 Apply/OK 重新加载。");
                updatePageInfo(0, 0);
            } else {
                readText(CURRENT);
            }
        } else {
            setText("没有文本文件路径...\n请到 Settings → Tools → Touch Fish 选择 txt 文件。");
            updatePageInfo(0, 0);
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
            updatePageInfo(0, 0);
            return;
        }

        List<String> allLines = settings.getLines();
        if (allLines == null || allLines.isEmpty()) {
            setText("当前没有可显示的文本内容。\n请到 Settings → Tools → Touch Fish 重新选择文件并点击 Apply/OK。");
            updatePageInfo(0, 0);
            return;
        }

        int pageSize = getPageSize(settings);
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

        showPage(settings, allLines, targetPage, pageSize, totalPage);
    }

    private void jumpToPage() {
        BookSettingsState settings = BookSettingsState.getInstance();
        if (settings == null || settings.getLines() == null || settings.getLines().isEmpty()) {
            showMessage("当前没有可跳转的文本内容。");
            return;
        }

        String input = jumpPageField.getText() == null ? "" : jumpPageField.getText().trim();
        if (input.isEmpty()) {
            jumpPageField.requestFocusInWindow();
            return;
        }

        final int requestedPage;
        try {
            requestedPage = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            showMessage("请输入有效的页码。");
            jumpPageField.selectAll();
            return;
        }

        int pageSize = getPageSize(settings);
        int totalPage = Math.max(1, (settings.getLines().size() + pageSize - 1) / pageSize);
        int targetPage = Math.max(1, Math.min(requestedPage, totalPage));

        if (requestedPage != targetPage) {
            showMessage("页码范围是 1 - " + totalPage + "，已跳转到第 " + targetPage + " 页。");
        }

        showPage(settings, settings.getLines(), targetPage, pageSize, totalPage);
        jumpPageField.setText("");
        text.requestFocusInWindow();
    }

    private void showPage(BookSettingsState settings,
                          List<String> allLines,
                          int targetPage,
                          int pageSize,
                          int totalPage) {
        settings.setPage(targetPage);
        settings.setTotalPage(totalPage);
        setText(readFromPage(allLines, targetPage, pageSize));
        updatePageInfo(targetPage, totalPage);
    }

    private int getPageSize(BookSettingsState settings) {
        return settings.getPageSize() == null || settings.getPageSize() < 1
                ? 3 : settings.getPageSize();
    }

    private void updatePageInfo(int currentPage, int totalPage) {
        pageInfoLabel.setText("第 " + currentPage + " / " + totalPage + " 页");
        jumpPageField.setEnabled(totalPage > 0);
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
