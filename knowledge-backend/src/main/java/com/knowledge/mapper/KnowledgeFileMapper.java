package com.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.knowledge.entity.KnowledgeFile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface KnowledgeFileMapper extends BaseMapper<KnowledgeFile> {

    IPage<KnowledgeFile> selectFilePage(
            IPage<KnowledgeFile> page,
            @Param("fileCategory") String fileCategory,
            @Param("keyword") String keyword,
            @Param("tagId") Long tagId
    );
}
