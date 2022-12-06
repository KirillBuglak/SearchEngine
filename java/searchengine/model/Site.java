package searchengine.model;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;

@Entity
public class Site{
    @Id//fixme autoincrement?
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @NotNull //fixme
//    @Column(columnDefinition = "ENUM('INDEXING', 'INDEXED', 'FAILED')") //fixme work on it now
    @Enumerated(EnumType.STRING)
    private Status status;
    @NotNull //fixme
    @Column(columnDefinition = "DATETIME") //fixme
    private Date statusTime; //(в случае статуса INDEXING дата и время должны обновляться регулярно
    // при добавлении каждой новой страницы в индекс)
    @Column(columnDefinition = "TEXT")
    private String lastError; //текст ошибки индексации или NULL, если её не было
    @NotNull //fixme
    @Column(columnDefinition = "VARCHAR(255)")
    private String url; //адрес главной страницы сайта
    @NotNull //fixme
    @Column(columnDefinition = "VARCHAR(255)")
    private String name;

    public Site() {
    }

    public Site(String url, String name) {
        this.status = Status.INDEXING;
        this.statusTime = new Date();
        this.lastError = "";
        this.url = url;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Date getStatusTime() {
        return statusTime;
    }

    public void setStatusTime(Date statusTime) {
        this.statusTime = statusTime;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
