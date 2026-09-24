package com.qareporting;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * Activates JAX-RS and roots every REST resource under /api — mirrors the
 * Laravel version's routes/api.php prefix.
 */
@ApplicationPath("/api")
public class ApiApplication extends Application {
}
