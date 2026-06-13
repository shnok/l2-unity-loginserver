package com.shnok.javaserver.db.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name = "ACCOUNTS")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DBAccountInfo {
    @Id
    @Column(name = "login")
    private String login;
    @Column(name = "password")
    private String passHash;
    @Column(name = "access_level")
    private int accessLevel;
    @Column(name = "last_server")
    private int lastServer;
    @Column(name = "last_ip")
    private String lastIp;
    @Column(name = "last_active")
    private Long lastActive;
}
