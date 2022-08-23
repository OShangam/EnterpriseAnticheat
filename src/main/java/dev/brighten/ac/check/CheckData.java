package dev.brighten.ac.check;

import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.packet.ProtocolVersion;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface CheckData {
    String name();
    CheckType type();

    int punishVl() default 10;

    ProtocolVersion minVersion() default ProtocolVersion.V1_7;
    ProtocolVersion maxVersion() default ProtocolVersion.V1_19;
}
