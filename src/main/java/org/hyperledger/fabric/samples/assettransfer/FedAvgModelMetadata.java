/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.samples.assettransfer;

import com.google.gson.Gson;
import com.owlike.genson.annotation.JsonProperty;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

@DataType()
public final class FedAvgModelMetadata {

    @Property()
    private final String modelId;
    @Property()
    private final String name;
    @Property()
    private final int clientsPerRound;
    @Property()
    private final String status;
    @Property()
    private final int trainingRounds;
    @Property()
    private final int currentRound;


    public FedAvgModelMetadata(@JsonProperty("modelId") final String modelId,
                               @JsonProperty("name") final String name,
                               @JsonProperty("clientsPerRound") final int clientsPerRound,
                               @JsonProperty("status") final String status,
                               @JsonProperty("trainingRounds") final int trainingRounds,
                               @JsonProperty("currentRound") final int currentRound) {
        this.modelId = modelId;
        this.name = name;
        this.clientsPerRound = clientsPerRound;
        this.status = status;
        this.trainingRounds = trainingRounds;
        this.currentRound = currentRound;
    }

    public String getModelId() {
        return modelId;
    }

    public int getClientsPerRound() {
        return clientsPerRound;
    }

    public String getName() {
        return name;
    }

    public String getStatus() {
        return status;
    }

    public int getTrainingRounds() {
        return trainingRounds;
    }

    public int getCurrentRound() {
        return currentRound;
    }

    public String serialize() {
        return new Gson().toJson(this);
    }

    public static FedAvgModelMetadata deserialize(final String ser) {
        return new Gson().fromJson(ser, FedAvgModelMetadata.class);
    }
}
