package com.ondoset.repository;

import com.ondoset.domain.Clothes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClothesRepository extends JpaRepository<Clothes, Long> {

	List<Clothes> findByIdIn(List<Long> idList);
}