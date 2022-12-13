package searchengine.model;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Comparator;

@Entity
public class Page implements Comparable<Page> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @ManyToOne()
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;
    @Column(columnDefinition = "TEXT not null, KEY(path(255))")
    private String path;
    @Column(nullable = false)
    private int code;
    @Column(columnDefinition = "MEDIUMTEXT", nullable = false)
    private String content; //fixme maybe StringBuilder
    @Transient
    private double relativeRelevance;

    public Page() {
    }

    public Page(Site site, String path, int code, String content) {
        this.site = site;
        this.path = path.equals(site.getUrl()) ? "/" : path;
        this.code = code;
        this.content = content;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Site getSite() {
        return site;
    }

    public void setSite(Site site) {
        this.site = site;
    }

    public String getPath() {
        return path.equals("/") ? getSite().getUrl() : getSite().getUrl() + path.substring(1);
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public double getRelativeRelevance() {
        return relativeRelevance;
    }

    public void setRelativeRelevance(double relativeRelevance) {
        this.relativeRelevance = relativeRelevance;
    }

    @Override
    public int compareTo(Page p) {
        return Comparator.comparing(Page::getRelativeRelevance)
                .thenComparing(Page::getId)
                .compare(this, p);
    }
}