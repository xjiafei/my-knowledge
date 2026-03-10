package com.knowledge.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("file_tag")
public class FileTag {

    private Long fileId;

    private Long tagId;
}
