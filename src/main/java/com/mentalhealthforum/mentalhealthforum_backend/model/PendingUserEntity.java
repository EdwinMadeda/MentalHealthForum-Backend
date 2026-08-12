package com.mentalhealthforum.mentalhealthforum_backend.model;

import com.mentalhealthforum.mentalhealthforum_backend.dto.novu.NovuSubscriberData;
import com.mentalhealthforum.mentalhealthforum_backend.dto.novu.NovuSubscriberRequest;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("pending_users")
public record PendingUserEntity(
        @Id
        Long id,

        @Column("username")
        String username,

        @Column("email")
        String email,

        @Column("encrypted_password")
        String encryptedPassword,

        @Column("first_name")
        String firstName,

         @Column("last_name")
        String lastName
){
        /**
         * Converts this pending user to a Novu subscriber request.
         * Uses email as the subscriber ID for simplicity.
         */
        public NovuSubscriberRequest toNovuSubscriberRequest(){
                return new NovuSubscriberRequest(
                        this.email, // subscriberId = email
                        this.firstName,
                        this.lastName,
                        this.email,
                        null, // avatarUrl – not available yet
                        "en",  // language – default
                        NovuSubscriberData.forUserInStaging()
                );
        }
}
