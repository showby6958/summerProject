package com.portfolio.memo.chatroom.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class InviteRequest {
    private List<String> emails;
}
