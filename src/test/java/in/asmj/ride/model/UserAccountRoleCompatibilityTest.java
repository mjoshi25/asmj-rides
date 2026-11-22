package in.asmj.ride.model;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class UserAccountRoleCompatibilityTest {
    @Test void legacyPrimaryRoleRemainsAnAuthorityWhenRolesFieldIsAbsent() {
        UserAccount user = new UserAccount();
        user.setRole("FLEET_OWNER");
        assertTrue(user.hasRole("FLEET_OWNER"));
        assertEquals(List.of("FLEET_OWNER"), user.getRoles());
    }

    @Test void vehicleOwnerAliasMapsToLegacyFleetOwnerAuthority() {
        UserAccount user = new UserAccount();
        user.setRole("VEHICLE_OWNER");
        assertEquals("FLEET_OWNER", user.getRole());
        assertTrue(user.hasRole("VEHICLE_OWNER"));
    }

    @Test void additionalRoleIsNotGrantedUntilAdminApproval() {
        UserAccount user = new UserAccount();
        user.setRole("CUSTOMER");
        user.addRole("TRAVEL_AGENT");
        assertFalse(user.getGrantedRoles().contains("TRAVEL_AGENT"));
        user.setRoleStatus("TRAVEL_AGENT", "APPROVED");
        assertTrue(user.getGrantedRoles().contains("TRAVEL_AGENT"));
        assertEquals("CUSTOMER", user.getRole());
    }

    @Test void additionalRolesCanBeStoredWithoutReplacingPrimaryRole() {
        UserAccount user = new UserAccount();
        user.setRole("FLEET_OWNER");
        user.addRole("TRAVEL_AGENT");
        assertEquals("FLEET_OWNER", user.getRole());
        assertTrue(user.hasRole("FLEET_OWNER"));
        assertTrue(user.hasRole("TRAVEL_AGENT"));
    }
}
