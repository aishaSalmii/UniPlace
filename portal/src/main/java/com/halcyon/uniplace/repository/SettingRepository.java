package com.halcyon.uniplace.repository;

import com.halcyon.uniplace.domain.SettingEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettingRepository extends JpaRepository<SettingEntry, String> {
}
