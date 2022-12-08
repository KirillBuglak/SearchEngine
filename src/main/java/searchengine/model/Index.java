package searchengine.model;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Entity
@Table(name = "`index`")
public class Index {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @ManyToOne()
    @JoinColumn(name = "page_id", nullable = false)
    private Page page;
    @ManyToOne()
    @JoinColumn(name = "lemma_id", nullable = false)
    private Lemma lemma;
    @Column(name = "`rank`", nullable = false)
    private float krank;

    public Index() {
    }

    public Index(Page page, Lemma lemma, float krank) {
        this.page = page;
        this.lemma = lemma;
        this.krank = krank;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Page getPage() {
        return page;
    }

    public void setPage(Page page) {
        this.page = page;
    }

    public Lemma getLemma() {
        return lemma;
    }

    public void setLemma(Lemma lemma) {
        this.lemma = lemma;
    }

    public float getRank() {
        return krank;
    }

    public void setRank(float krank) {
        this.krank = krank;
    }
}
