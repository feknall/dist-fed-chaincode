package org.hyperledger.fabric.samples.assettransfer;

import com.owlike.genson.annotation.JsonProperty;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import java.util.List;

@DataType()
public final class OriginalModelList {

    @Property()
    private final List<OriginalModel> originalModelList;

    public OriginalModelList(@JsonProperty("originalModelList") final List<OriginalModel> originalModelList) {
        this.originalModelList = originalModelList;
    }

    public List<OriginalModel> getOriginalModelList() {
        return originalModelList;
    }
}
