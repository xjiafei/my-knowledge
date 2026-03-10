package com.knowledge.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("note_tag")
public class NoteTag {

    private Long noteId;

    private Long tagId;

    public NoteTag() {}

    public NoteTag(Long noteId, Long tagId) {
        this.noteId = noteId;
        this.tagId = tagId;
    }
}
