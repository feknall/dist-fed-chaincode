/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.samples.assettransfer;

import com.owlike.genson.annotation.JsonProperty;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import java.util.Objects;

@DataType()
public final class Client {

    @Property()
    private final String clientID;

    @Property()
    private final String name;

    @Property()
    private final String cinNumber;

    @Property()
    private final boolean alive;

    public String getClientID() {
        return clientID;
    }

    public String getName() {
        return name;
    }

    public String getCinNumber() {
        return cinNumber;
    }

    public boolean isAlive() {
        return alive;
    }

    public Client(@JsonProperty("clientID") final String clientID,
                  @JsonProperty("name") final String name,
                  @JsonProperty("cinNumber") final String cinNumber,
                  @JsonProperty("alive") final boolean alive) {
        this.name = name;
        this.clientID = clientID;
        this.cinNumber = cinNumber;
        this.alive = alive;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }

        Client other = (Client) obj;

        return Objects.deepEquals(
                new String[]{getClientID(), getName(), getCinNumber()},
                new String[]{other.getClientID(), other.getName(), getCinNumber()})
                &&
                isAlive() == other.isAlive();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClientID(), getName(), getCinNumber(), isAlive());
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "@" + Integer.toHexString(hashCode())
                + " [clientID=" + clientID
                + ", name=" + name
                + ", cinNumber = " + cinNumber
                + ", alive = " + alive
                + "]";
    }
}
