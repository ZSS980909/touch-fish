package cn.luojunhui.touchfish.windwos;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

/**
 * Touch Fish tool window factory.
 */
public class BookWindowFactory implements ToolWindowFactory {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        Book book = new Book(toolWindow);
        Content content = ContentFactory.getInstance().createContent(book.getContent(), "", false);
        toolWindow.getContentManager().addContent(content);
    }
}
