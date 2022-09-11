/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.samples.assettransfer;

import com.google.gson.Gson;
import com.owlike.genson.annotation.JsonProperty;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

@DataType()
public final class ModelSecret {
    @Property()
    private final String modelId;
    @Property()
    private final int round;
    @Property()
    private final String weights;
    @Property()
    private final int datasetSize;

    public ModelSecret(@JsonProperty("modelId") final String modelId,
                       @JsonProperty("round") final int round,
                       @JsonProperty("weights") final String weights,
                       @JsonProperty("datasetSize") final int datasetSize) {
        this.modelId = modelId;
        this.round = round;
        this.weights = weights;
        this.datasetSize = datasetSize;
    }

    public String getModelId() {
        return modelId;
    }

    public int getRound() {
        return round;
    }

    public String getWeights() {
        return weights;
    }

    public int getDatasetSize() {
        return datasetSize;
    }

    public String serialize() {
        return new Gson().toJson(this);
    }

    public static ModelSecret deserialize(final String json) {
        return new Gson().fromJson(json, ModelSecret.class);
    }

}
