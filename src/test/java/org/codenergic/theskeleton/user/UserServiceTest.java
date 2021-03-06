package org.codenergic.theskeleton.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

import org.codenergic.theskeleton.role.RoleEntity;
import org.codenergic.theskeleton.role.RolePrivilegeRepository;
import org.codenergic.theskeleton.role.RoleRepository;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

public class UserServiceTest {
	private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);
	@Mock
	private RoleRepository roleRepository;
	@Mock
	private UserRepository userRepository;
	@Mock
	private UserRoleRepository userRoleRepository;
	@Mock
	private RolePrivilegeRepository rolePrivilegeRepository;
	private UserAdminService userAdminService;
	private UserService userService;

	@Before
	public void init() {
		MockitoAnnotations.initMocks(this);
		this.userAdminService = UserAdminService.newInstance(passwordEncoder, roleRepository, userRepository, userRoleRepository, rolePrivilegeRepository);
		this.userService = UserService.newInstance(passwordEncoder, roleRepository, userRepository, userRoleRepository, rolePrivilegeRepository);
	}

	@Test
	public void testAddRoleToUser() {
		RoleEntity role = new RoleEntity()
				.setId(UUID.randomUUID().toString())
				.setCode("role");
		UserEntity user = new UserEntity()
				.setId(UUID.randomUUID().toString())
				.setUsername("user")
				.setPassword(passwordEncoder.encode("user"));
		UserRoleEntity result = new UserRoleEntity(user, role);
		result.setId(UUID.randomUUID().toString());
		when(roleRepository.findByCode("role")).thenReturn(role);
		when(userRepository.findByUsername("user")).thenReturn(user);
		when(userRoleRepository.save(any(UserRoleEntity.class))).thenReturn(result);
		assertThat(userAdminService.addRoleToUser("user", "role")).isEqualTo(user);
		verify(roleRepository).findByCode("role");
		verify(userRepository).findByUsername("user");
		verify(userRoleRepository).save(any(UserRoleEntity.class));
	}

	@Test
	public void testDeleteUser() {
		UserEntity user = new UserEntity()
				.setId(UUID.randomUUID().toString())
				.setUsername("user")
				.setPassword(passwordEncoder.encode("user"));
		when(userRepository.findByUsername("user")).thenReturn(user);
		userAdminService.deleteUser("user");
		verify(userRepository).findByUsername("user");
		verify(userRepository).delete(user);
	}

	@Test
	@SuppressWarnings("serial")
	public void testEnableOrDisableUser() {
		UserEntity input = new UserEntity() {{ setId(UUID.randomUUID().toString()); }}
				.setUsername("user")
				.setEnabled(false);
		when(userRepository.findByUsername("user")).thenReturn(input);
		UserEntity result = userAdminService.enableOrDisableUser("user", true);
		assertThat(result.getId()).isEqualTo(input.getId());
		assertThat(result.isEnabled()).isTrue();
		verify(userRepository).findByUsername("user");
	}

	@Test
	public void testExtendsUserExpiration() {
		Date expiredAt = Calendar.getInstance(TimeZone.getDefault()).getTime();
		UserEntity input = new UserEntity()
				.setId(UUID.randomUUID().toString())
				.setUsername("user")
				.setExpiredAt(expiredAt);
		when(userRepository.findByUsername("user")).thenReturn(input);
		UserEntity result = userAdminService.extendsUserExpiration("user", 60);
		assertThat(result.getId()).isEqualTo(input.getId());
		assertThat(result.getExpiredAt()).isAfter(expiredAt);
		assertThat((result.getExpiredAt().getTime() - expiredAt.getTime()) / 1000).isEqualTo(3600);
		verify(userRepository).findByUsername("user");
	}

	@Test
	public void testFindRolesByUserUsername() {
		Set<UserRoleEntity> dbResult =
				new HashSet<>(Arrays.asList(new UserRoleEntity().setRole(new RoleEntity().setCode("role"))));
		when(userRoleRepository.findByUserUsername("user")).thenReturn(dbResult);
		Set<RoleEntity> result = userAdminService.findRolesByUserUsername("user");
		assertThat(result.size()).isEqualTo(1);
		assertThat(result.iterator().next()).isEqualTo(dbResult.iterator().next().getRole());
		verify(userRoleRepository).findByUserUsername("user");
	}

	@Test
	public void testFindUserByEmail() {
		UserEntity dbResult = new UserEntity().setUsername("user");
		when(userRepository.findByEmail("user@localhost")).thenReturn(dbResult);
		UserEntity result = userService.findUserByEmail("user@localhost");
		assertThat(result).isEqualTo(dbResult);
		verify(userRepository).findByEmail("user@localhost");
	}

	@Test
	public void testFindUsersByUsernameStartingWith() {
		Page<UserEntity> dbResult = new PageImpl<>(Arrays.asList(new UserEntity().setUsername("user")));
		when(userRepository.findByUsernameStartingWith(eq("user"), any()))
				.thenReturn(dbResult);
		Page<UserEntity> result = userAdminService.findUsersByUsernameStartingWith("user", null);
		assertThat(result.getNumberOfElements()).isEqualTo(1);
		assertThat(result).isEqualTo(dbResult);
		verify(userRepository).findByUsernameStartingWith(eq("user"), any());
	}

	@Test
	public void testLockOrUnlockUser() {
		UserEntity dbResult = new UserEntity().setAccountNonLocked(false);
		when(userRepository.findByUsername("user")).thenReturn(dbResult);
		UserEntity result = userAdminService.lockOrUnlockUser("user", true);
		assertThat(result.isAccountNonLocked()).isEqualTo(true);
		verify(userRepository).findByUsername("user");
	}

	@Test
	public void testRemoveRoleFromUser() {
		userAdminService.removeRoleFromUser("", "");
	}

	@Test
	public void testSaveUser() {
		userAdminService.saveUser(new UserEntity());
	}

	@Test
	public void testUpdateUser() {
		UserEntity input = new UserEntity()
				.setUsername("user")
				.setEnabled(false);
		when(userRepository.findByUsername("user")).thenReturn(input);
		UserEntity updatedUser = userAdminService.updateUser("user", new UserEntity().setUsername("updated"));
		assertThat(updatedUser.getUsername()).isEqualTo(input.getUsername());
		verify(userRepository).findByUsername("user");
	}

	@Test
	public void testUpdateUserPassword() {
		String rawPassword = "p@$$w0rd!";
		when(userRepository.findByUsername("user")).thenReturn(new UserEntity());
		UserEntity result = userAdminService.updateUserPassword("user", rawPassword);
		assertThat(passwordEncoder.matches(rawPassword, result.getPassword())).isTrue();
		verify(userRepository).findByUsername("user");
	}
}
