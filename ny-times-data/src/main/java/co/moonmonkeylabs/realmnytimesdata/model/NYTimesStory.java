package co.moonmonkeylabs.realmnytimesdata.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NYTimesStory extends RealmObject {

    @JsonProperty("section")
    private String section;

    @JsonProperty("subsection")
    private String subsection;

    @JsonProperty("title")
    private String title;

    @JsonProperty("abstract")
    private String storyAbstract;

    @PrimaryKey
    @JsonProperty("url")
    private String url;

    @JsonProperty("byline")
    private String byline;

    @JsonProperty("item_type")
    private String itemType;

    @JsonProperty("updated_date")
    private String updatedDate;

    @JsonProperty("created_date")
    private String createdDate;

    @JsonProperty("published_date")
    private String publishedDate;

    @JsonProperty("material_type_facet")
    private String materialTypeFacet;

    @JsonProperty("kicker")
    private String kicker;

    @JsonProperty("multimedia")
    private RealmList<NYTimesMultimedium> multimedia;

    private long sortTimeStamp;

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public String getSubsection() {
        return subsection;
    }

    public void setSubsection(String subsection) {
        this.subsection = subsection;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getStoryAbstract() {
        return storyAbstract;
    }

    public void setStoryAbstract(String storyAbstract) {
        this.storyAbstract = storyAbstract;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getByline() {
        return byline;
    }

    public void setByline(String byline) {
        this.byline = byline;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public String getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(String updatedDate) {
        this.updatedDate = updatedDate;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }

    public String getPublishedDate() {
        return publishedDate;
    }

    public void setPublishedDate(String publishedDate) {
        this.publishedDate = publishedDate;
    }

    public String getMaterialTypeFacet() {
        return materialTypeFacet;
    }

    public void setMaterialTypeFacet(String materialTypeFacet) {
        this.materialTypeFacet = materialTypeFacet;
    }

    public String getKicker() {
        return kicker;
    }

    public void setKicker(String kicker) {
        this.kicker = kicker;
    }

    public RealmList<NYTimesMultimedium> getMultimedia() {
        return multimedia;
    }

    @JsonDeserialize(using = RealmListNYTimesMultimediumDeserializer.class)
    public void setMultimedia(RealmList<NYTimesMultimedium> multimedia) {
        this.multimedia = multimedia;
    }

    public long getSortTimeStamp() {
        return sortTimeStamp;
    }

    public void setSortTimeStamp(long sortTimeStamp) {
        this.sortTimeStamp = sortTimeStamp;
    }
}
