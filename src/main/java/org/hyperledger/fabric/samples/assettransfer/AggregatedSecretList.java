package org.hyperledger.fabric.samples.assettransfer;

import com.owlike.genson.annotation.JsonProperty;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import java.util.List;

@DataType
public final class AggregatedSecretList {
    @Property()
    private final List<AggregatedSecret> aggregatedSecretList;

    public AggregatedSecretList(@JsonProperty("aggregatedSecretList") final List<AggregatedSecret> aggregatedSecretList) {
        this.aggregatedSecretList = aggregatedSecretList;
    }

    public List<AggregatedSecret> getAggregatedSecretList() {
        return aggregatedSecretList;
    }
}
