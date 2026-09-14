package cn.luojunhui.touchfish.config;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 持久存储自定义设置。
 */
@State(
        name = "cn.luojunhui.touchfish.config.BookSettingsState",
        storages = {@Storage("TouchFishSettingsPlugin.xml")}
)
public class BookSettingsState implements PersistentStateComponent<BookSettingsState> {
    private String bookPath = "";
    private Integer page = 1;
    private Integer pageSize = 3;
    private Integer totalPage = 0;
    private List<String> lines = new ArrayList<>();

    public static BookSettingsState getInstance() {
        return ApplicationManager.getApplication().getService(BookSettingsState.class);
    }

    @Nullable
    @Override
    public BookSettingsState getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull BookSettingsState state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    public String getBookPath() {
        return bookPath;
    }

    public void setBookPath(String bookPath) {
        this.bookPath = bookPath;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public Integer getTotalPage() {
        return totalPage;
    }

    public void setTotalPage(Integer totalPage) {
        this.totalPage = totalPage;
    }

    public List<String> getLines() {
        return lines;
    }

    public void setLines(List<String> lines) {
        this.lines = lines;
    }
}
