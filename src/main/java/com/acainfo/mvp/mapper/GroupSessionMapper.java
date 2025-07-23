package com.acainfo.mvp.mapper;

import com.acainfo.mvp.dto.coursegroup.GroupSessionDto;
import com.acainfo.mvp.model.GroupSession;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class GroupSessionMapper {

    public GroupSessionDto toDto(GroupSession groupSession) {
        return GroupSessionDto.builder()
                .id(groupSession.getId())
                .dayOfWeek(groupSession.getDayOfWeek().toString())
                .endTime(groupSession.getEndTime())
                .startTime(groupSession.getStartTime())
                .classroom(groupSession.getClassroom())
                .build();
    }

    public List<GroupSessionDto> toDtoList(List<GroupSession> groupSessions) {
        if (groupSessions == null) {
            return new ArrayList<>();
        }

        return groupSessions.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
}
