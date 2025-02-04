package com.hunzz.concertserver.repository

import com.hunzz.concertserver.model.Concert
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ConcertRepository : JpaRepository<Concert, Long>