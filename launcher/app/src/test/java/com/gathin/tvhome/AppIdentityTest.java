package com.gathin.tvhome;
import org.junit.Test;
import static org.junit.Assert.*;
public class AppIdentityTest {
    @Test public void xuperLabelOverridesLegacyPackage(){assertEquals("xuper",AppIdentity.brand("com.magistv.player","Xuper TV"));}
    @Test public void magisIsNotAssumedToBeXuper(){assertEquals("installed",AppIdentity.brand("com.magistv.player","Magis TV"));}
    @Test public void similarNamesDoNotImpersonateKnownPackages(){assertEquals("installed",AppIdentity.brand("example.fake","YouTube Premium"));}
}
