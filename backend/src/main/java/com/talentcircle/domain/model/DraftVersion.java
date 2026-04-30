package com.talentcircle.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity`
@Table(name = "draft_versions")
public class DraftVersion extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "draft_id")
    private Draft draft;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "edited_by")
    private String editedBy;

    @Column(name = "version_number")
    private Integer versionNumber = 1;

    // Getters and Setters
    public Draft getDraft() { return draft; }
    public void setDraft(Draft draft) { this.draft = draft; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getEditedBy() { return editedBy; }
    public void setEditedBy(String editedBy) { this.editedBy = editedBy; }

    public Integer getVersionNumber() { return versionNumber; }
    public void setVersionNumber(Integer versionNumber) { this.versionNumber = versionNumber; }
}
