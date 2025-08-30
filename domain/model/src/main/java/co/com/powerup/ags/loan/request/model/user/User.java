package co.com.powerup.ags.loan.request.model.user;

import java.math.BigDecimal;
import java.time.LocalDate;

public class User {
    
    private final String id;
    private final String name;
    private final String lastName;
    private final String address;
    private final String phoneNumber;
    private final LocalDate birthDate;
    private final String email;
    private final BigDecimal baseSalary;
    private final String idNumber;
    
    private User(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.lastName = builder.lastName;
        this.address = builder.address;
        this.phoneNumber = builder.phoneNumber;
        this.birthDate = builder.birthDate;
        this.email = builder.email;
        this.baseSalary = builder.baseSalary;
        this.idNumber = builder.idNumber;
    }
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public String getLastName() {
        return lastName;
    }
    
    public String getAddress() {
        return address;
    }
    
    public String getPhoneNumber() {
        return phoneNumber;
    }
    
    public LocalDate getBirthDate() {
        return birthDate;
    }
    
    public String getEmail() {
        return email;
    }
    
    public BigDecimal getBaseSalary() {
        return baseSalary;
    }
    
    public String getIdNumber() {
        return idNumber;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String id;
        private String name;
        private String lastName;
        private String address;
        private String phoneNumber;
        private LocalDate birthDate;
        private String email;
        private BigDecimal baseSalary;
        private String idNumber;
        
        private Builder() {}
        
        public Builder id(String id) {
            this.id = id;
            return this;
        }
        
        public Builder name(String name) {
            this.name = name;
            return this;
        }
        
        public Builder lastName(String lastName) {
            this.lastName = lastName;
            return this;
        }
        
        public Builder address(String address) {
            this.address = address;
            return this;
        }
        
        public Builder phoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
            return this;
        }
        
        public Builder birthDate(LocalDate birthDate) {
            this.birthDate = birthDate;
            return this;
        }
        
        public Builder email(String email) {
            this.email = email;
            return this;
        }
        
        public Builder baseSalary(BigDecimal baseSalary) {
            this.baseSalary = baseSalary;
            return this;
        }
        
        public Builder idNumber(String idNumber) {
            this.idNumber = idNumber;
            return this;
        }
        
        public User build() {
            return new User(this);
        }
    }
}
