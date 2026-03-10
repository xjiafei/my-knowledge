package com.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.knowledge.common.BusinessException;
import com.knowledge.dto.TagDTO;
import com.knowledge.dto.TagRequest;
import com.knowledge.entity.Tag;
import com.knowledge.mapper.TagMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TagService {

    private final TagMapper tagMapper;

    public List<TagDTO> listAll() {
        return tagMapper.selectAllWithNoteCount();
    }

    @Transactional(rollbackFor = Exception.class)
    public TagDTO createTag(TagRequest request) {
        String name = request.getName().trim();
        // Check for duplicate name
        Long existCount = tagMapper.selectCount(
                new LambdaQueryWrapper<Tag>().eq(Tag::getName, name));
        if (existCount > 0) {
            throw BusinessException.conflict("标签名 '" + name + "' 已存在");
        }

        Tag tag = new Tag();
        tag.setName(name);
        tagMapper.insert(tag);

        log.info("Created tag: id={}, name={}", tag.getId(), tag.getName());

        TagDTO dto = new TagDTO();
        dto.setId(tag.getId());
        dto.setName(tag.getName());
        dto.setNoteCount(0);
        dto.setCreatedAt(tag.getCreatedAt());
        return dto;
    }

    @Transactional(rollbackFor = Exception.class)
    public TagDTO updateTag(Long id, TagRequest request) {
        Tag tag = tagMapper.selectById(id);
        if (tag == null) {
            throw BusinessException.notFound("标签不存在: id=" + id);
        }

        String newName = request.getName().trim();
        if (!newName.equals(tag.getName())) {
            Long existCount = tagMapper.selectCount(
                    new LambdaQueryWrapper<Tag>().eq(Tag::getName, newName));
            if (existCount > 0) {
                throw BusinessException.conflict("标签名 '" + newName + "' 已存在");
            }
        }

        tag.setName(newName);
        tagMapper.updateById(tag);

        log.info("Updated tag: id={}, name={}", tag.getId(), tag.getName());

        TagDTO dto = tagMapper.selectWithNoteCountById(id);
        if (dto == null) {
            dto = new TagDTO();
            dto.setId(tag.getId());
            dto.setName(tag.getName());
            dto.setNoteCount(0);
            dto.setCreatedAt(tag.getCreatedAt());
        }
        return dto;
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteTag(Long id) {
        Tag tag = tagMapper.selectById(id);
        if (tag == null) {
            throw BusinessException.notFound("标签不存在: id=" + id);
        }
        tagMapper.deleteById(id);
        log.info("Deleted tag: id={}", id);
    }
}
