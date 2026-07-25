package com.lottery.controller;

import com.lottery.dto.ApiResponse;
import com.lottery.dto.CheckinResultDTO;
import com.lottery.dto.UserDTO;
import com.lottery.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "用户接口", description = "用户相关的API接口")
public class UserController {

    private final UserService userService;

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "用户登录验证")
    public ApiResponse<UserDTO> login(
            @Parameter(description = "用户名") @RequestParam String username,
            @Parameter(description = "密码") @RequestParam String password) {
        UserDTO user = userService.login(username, password);
        return ApiResponse.success("登录成功", user);
    }

    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "新用户注册")
    public ApiResponse<UserDTO> register(
            @Parameter(description = "用户名") @RequestParam String username,
            @Parameter(description = "密码") @RequestParam String password,
            @Parameter(description = "昵称") @RequestParam(required = false) String nickname,
            @Parameter(description = "手机号") @RequestParam(required = false) String phone) {
        UserDTO user = userService.register(username, password, nickname, phone);
        return ApiResponse.success("注册成功", user);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取用户信息", description = "根据ID获取用户信息")
    public ApiResponse<UserDTO> getUserById(
            @Parameter(description = "用户ID") @PathVariable Long id) {
        UserDTO user = userService.getUserById(id);
        return ApiResponse.success(user);
    }

    @GetMapping("/username/{username}")
    @Operation(summary = "根据用户名获取用户", description = "根据用户名获取用户信息")
    public ApiResponse<UserDTO> getUserByUsername(
            @Parameter(description = "用户名") @PathVariable String username) {
        UserDTO user = userService.getUserByUsername(username);
        return ApiResponse.success(user);
    }

    @GetMapping
    @Operation(summary = "获取所有用户", description = "分页获取所有用户")
    public ApiResponse<Page<UserDTO>> getAllUsers(
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size) {
        Page<UserDTO> users = userService.getAllUsers(page, size);
        return ApiResponse.success(users);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新用户信息", description = "更新用户信息")
    public ApiResponse<UserDTO> updateUser(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @Valid @RequestBody UserDTO userDTO) {
        UserDTO user = userService.updateUser(id, userDTO);
        return ApiResponse.success("更新成功", user);
    }

    @PostMapping("/{id}/add-chances")
    @Operation(summary = "增加抽奖次数", description = "为用户增加抽奖次数")
    public ApiResponse<UserDTO> addChances(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @Parameter(description = "增加数量") @RequestParam int count) {
        UserDTO user = userService.addChances(id, count);
        return ApiResponse.success("增加成功", user);
    }

    @PatchMapping("/{id}/toggle")
    @Operation(summary = "切换用户状态", description = "切换用户的启用/禁用状态")
    public ApiResponse<UserDTO> toggleEnabled(
            @Parameter(description = "用户ID") @PathVariable Long id) {
        UserDTO user = userService.toggleEnabled(id);
        return ApiResponse.success("状态切换成功", user);
    }

    @PostMapping("/{id}/checkin")
    @Operation(summary = "每日签到", description = "用户每日签到，签到成功增加1次抽奖机会")
    public ApiResponse<CheckinResultDTO> dailyCheckin(
            @Parameter(description = "用户ID") @PathVariable Long id) {
        CheckinResultDTO result = userService.dailyCheckin(id);
        return ApiResponse.success(result.getMessage(), result);
    }

    @GetMapping("/{id}/checkin-status")
    @Operation(summary = "获取签到状态", description = "获取用户今日签到状态")
    public ApiResponse<CheckinResultDTO> getCheckinStatus(
            @Parameter(description = "用户ID") @PathVariable Long id) {
        CheckinResultDTO result = userService.getCheckinStatus(id);
        return ApiResponse.success(result);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除用户", description = "删除指定用户")
    public ApiResponse<Void> deleteUser(
            @Parameter(description = "用户ID") @PathVariable Long id) {
        userService.deleteUser(id);
        return ApiResponse.success("删除成功", null);
    }
}
