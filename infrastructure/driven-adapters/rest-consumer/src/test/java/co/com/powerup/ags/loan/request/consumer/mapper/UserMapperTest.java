package co.com.powerup.ags.loan.request.consumer.mapper;

import co.com.powerup.ags.loan.request.consumer.api.model.User;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserMapperTest {

    private static final String CARLOS_EMAIL = "carlos.rodriguez@ejemplo.com";
    private static final String MARIA_EMAIL = "maria.fernandez@ejemplo.com";
    private static final String CARLOS_NAME = "Carlos";
    private static final String CARLOS_LASTNAME = "Rodriguez";
    private static final String MARIA_NAME = "Maria";
    private static final String MARIA_LASTNAME = "Fernandez";
    private static final String VALID_USER_ID_1 = "12345678";
    private static final String VALID_USER_ID_2 = "87654321";
    private static final BigDecimal BASE_SALARY_5000 = new BigDecimal("5000.00");
    private static final BigDecimal BASE_SALARY_6000 = new BigDecimal("6000.00");
    private static final String ADDRESS_BOGOTA = "Calle 123, Bogota";
    private static final String ADDRESS_MEDELLIN = "Carrera 456, Medellin";
    private static final String PHONE_NUMBER_1 = "3001234567";
    private static final String PHONE_NUMBER_2 = "3009876543";
    private static final LocalDate BIRTH_DATE_1990 = LocalDate.of(1990, 1, 1);
    private static final LocalDate BIRTH_DATE_1985 = LocalDate.of(1985, 5, 15);

    @Test
    void shouldMapApiUserToDomain() {
        String userId = UUID.randomUUID().toString();
        User apiUser = User.builder()
                .id(userId)
                .name(CARLOS_NAME)
                .lastName(CARLOS_LASTNAME)
                .email(CARLOS_EMAIL)
                .idNumber(VALID_USER_ID_1)
                .baseSalary(BASE_SALARY_5000)
                .address(ADDRESS_BOGOTA)
                .phoneNumber(PHONE_NUMBER_1)
                .birthDate(BIRTH_DATE_1990)
                .build();

        co.com.powerup.ags.loan.request.model.user.User domainUser = UserMapper.INSTANCE.toDomain(apiUser);

        assertNotNull(domainUser);
        assertEquals(userId, domainUser.getId());
        assertEquals(CARLOS_NAME, domainUser.getName());
        assertEquals(CARLOS_LASTNAME, domainUser.getLastName());
        assertEquals(CARLOS_EMAIL, domainUser.getEmail());
        assertEquals(VALID_USER_ID_1, domainUser.getIdNumber());
        assertEquals(BASE_SALARY_5000, domainUser.getBaseSalary());
        assertEquals(ADDRESS_BOGOTA, domainUser.getAddress());
        assertEquals(PHONE_NUMBER_1, domainUser.getPhoneNumber());
        assertEquals(BIRTH_DATE_1990, domainUser.getBirthDate());
    }

    @Test
    void shouldMapDifferentApiUserToDomain() {
        String userId = UUID.randomUUID().toString();
        User apiUser = User.builder()
                .id(userId)
                .name(MARIA_NAME)
                .lastName(MARIA_LASTNAME)
                .email(MARIA_EMAIL)
                .idNumber(VALID_USER_ID_2)
                .baseSalary(BASE_SALARY_6000)
                .address(ADDRESS_MEDELLIN)
                .phoneNumber(PHONE_NUMBER_2)
                .birthDate(BIRTH_DATE_1985)
                .build();

        co.com.powerup.ags.loan.request.model.user.User domainUser = UserMapper.INSTANCE.toDomain(apiUser);

        assertNotNull(domainUser);
        assertEquals(userId, domainUser.getId());
        assertEquals(MARIA_NAME, domainUser.getName());
        assertEquals(MARIA_LASTNAME, domainUser.getLastName());
        assertEquals(MARIA_EMAIL, domainUser.getEmail());
        assertEquals(VALID_USER_ID_2, domainUser.getIdNumber());
        assertEquals(BASE_SALARY_6000, domainUser.getBaseSalary());
        assertEquals(ADDRESS_MEDELLIN, domainUser.getAddress());
        assertEquals(PHONE_NUMBER_2, domainUser.getPhoneNumber());
        assertEquals(BIRTH_DATE_1985, domainUser.getBirthDate());
    }

    @Test
    void shouldMapNullApiUserToNull() {
        co.com.powerup.ags.loan.request.model.user.User domainUser = UserMapper.INSTANCE.toDomain(null);

        assertNull(domainUser);
    }

    @Test
    void shouldMapApiUserWithNullFields() {
        User apiUser = User.builder()
                .id(null)
                .name(null)
                .lastName(null)
                .email(CARLOS_EMAIL)
                .idNumber(VALID_USER_ID_1)
                .baseSalary(null)
                .address(null)
                .phoneNumber(null)
                .birthDate(null)
                .build();

        co.com.powerup.ags.loan.request.model.user.User domainUser = UserMapper.INSTANCE.toDomain(apiUser);

        assertNotNull(domainUser);
        assertNull(domainUser.getId());
        assertNull(domainUser.getName());
        assertNull(domainUser.getLastName());
        assertEquals(CARLOS_EMAIL, domainUser.getEmail());
        assertEquals(VALID_USER_ID_1, domainUser.getIdNumber());
        assertNull(domainUser.getBaseSalary());
        assertNull(domainUser.getAddress());
        assertNull(domainUser.getPhoneNumber());
        assertNull(domainUser.getBirthDate());
    }

    @Test
    void shouldMapApiUserWithMinimalData() {
        User apiUser = User.builder()
                .email(MARIA_EMAIL)
                .idNumber(VALID_USER_ID_2)
                .build();

        co.com.powerup.ags.loan.request.model.user.User domainUser = UserMapper.INSTANCE.toDomain(apiUser);

        assertNotNull(domainUser);
        assertEquals(MARIA_EMAIL, domainUser.getEmail());
        assertEquals(VALID_USER_ID_2, domainUser.getIdNumber());
        assertNull(domainUser.getId());
        assertNull(domainUser.getName());
        assertNull(domainUser.getLastName());
        assertNull(domainUser.getBaseSalary());
        assertNull(domainUser.getAddress());
        assertNull(domainUser.getPhoneNumber());
        assertNull(domainUser.getBirthDate());
    }

    @Test
    void shouldMapEmptyApiUser() {
        User apiUser = User.builder().build();

        co.com.powerup.ags.loan.request.model.user.User domainUser = UserMapper.INSTANCE.toDomain(apiUser);

        assertNotNull(domainUser);
        assertNull(domainUser.getId());
        assertNull(domainUser.getName());
        assertNull(domainUser.getLastName());
        assertNull(domainUser.getEmail());
        assertNull(domainUser.getIdNumber());
        assertNull(domainUser.getBaseSalary());
        assertNull(domainUser.getAddress());
        assertNull(domainUser.getPhoneNumber());
        assertNull(domainUser.getBirthDate());
    }
}