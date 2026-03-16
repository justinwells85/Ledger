package com.ledger.entity;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * System-level configuration values (tolerance thresholds, aging periods).
 * Spec: 10-business-rules.md BR-31, BR-32
 */
@Entity
@Table(name = "system_config")
public class SystemConfig {

    @Id
    @Column(name = "config_key", length = 100)
    private String configKey;

    @Column(name = "config_value", nullable = false, length = 500)
    private String configValue;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "data_type", nullable = false, length = 20)
    private String dataType = "TEXT";

    @Column(name = "display_group", nullable = false, length = 100)
    private String displayGroup = "GENERAL";

    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName = "";

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by", nullable = false, length = 100)
    private String updatedBy;

    public SystemConfig() {
    }

    public String getConfigKey() { return configKey; }
    public void setConfigKey(String configKey) { this.configKey = configKey; }

    public String getConfigValue() { return configValue; }
    public void setConfigValue(String configValue) { this.configValue = configValue; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDataType() { return dataType; }
    public void setDataType(String dataType) { this.dataType = dataType; }

    public String getDisplayGroup() { return displayGroup; }
    public void setDisplayGroup(String displayGroup) { this.displayGroup = displayGroup; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}
