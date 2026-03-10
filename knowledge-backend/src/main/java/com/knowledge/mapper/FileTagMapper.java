package com.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.dto.TagDTO;
import com.knowledge.entity.FileTag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FileTagMapper extends BaseMapper<FileTag> {

    List<TagDTO> selectTagsByFileId(@Param("fileId") Long fileId);

    void batchInsert(@Param("fileId") Long fileId, @Param("tagIds") List<Long> tagIds);
}
