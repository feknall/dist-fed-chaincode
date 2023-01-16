/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.samples.assettransfer;

import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contact;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Default;
import org.hyperledger.fabric.contract.annotation.Info;
import org.hyperledger.fabric.contract.annotation.License;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.CompositeKey;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


@Contract(
        name = "fedavg-chaincode",
        info = @Info(
                title = "Federated Learning",
                description = "Privacy-preserving federated learning",
                version = "0.0.1-SNAPSHOT",
                license = @License(
                        name = "Apache 2.0 License",
                        url = "http://www.apache.org/licenses/LICENSE-2.0.html"),
                contact = @Contact(
                        email = "h.fazli.k@gmail.com",
                        name = "Hamid Fazli",
                        url = "https://hyperledger.example.com")))
@Default
public final class FedAvgChaincode implements ContractInterface {

    private final Logger logger = Logger.getLogger(getClass().toString());

    private enum ChaincodeErrors {
        MODEL_NOT_FOUND,
        MODEL_METADATA_ALREADY_EXISTS,
        MODEL_METADATA_DOES_NOT_EXISTS,
        TRAINING_NOT_FINISHED,
        INVALID_ACCESS
    }

    static final String MODEL_METADATA_KEY = "modelMetadataKey";
    static final String MODEL_KEY = "originalModelKey";
    static final String END_ROUND_MODEL_KEY = "endRoundModelKey";
    static final String CLIENT_SELECTED_FOR_ROUND_KEY = "clientSelectedForRoundKey";
    static final String LEAD_AGGREGATOR_ATTRIBUTE = "leadAggregator";
    static final String TRAINER_ATTRIBUTE = "trainer";
    static final String FL_ADMIN_ATTRIBUTE = "flAdmin";
    static final String ENROLMENT_ID_ATTRIBUTE_KEY = "hf.EnrollmentID";

    static final String ROUND_FINISHED_EVENT = "ROUND_FINISHED_EVENT";
    static final String TRAINING_FINISHED_EVENT = "TRAINING_FINISHED_EVENT";
    static final String ORIGINAL_MODEL_ADDED_EVENT = "ORIGINAL_MODEL_ADDED_EVENT";
    static final String START_TRAINING_EVENT = "START_TRAINING_EVENT";
    static final String CREATE_MODEL_METADATA_EVENT = "CREATE_MODEL_METADATA_EVENT";

    static final String MODEL_METADATA_STATUS_INITIATED = "initiated";
    static final String MODEL_METADATA_STATUS_STARTED = "started";
    static final String MODEL_METADATA_STATUS_FINISHED = "finished";

    // ------------------- START ADMIN ---------------------
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void initLedger(final Context ctx) {

    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public FedAvgModelMetadata startTraining(final Context ctx, final String modelId) {
        checkHasFlAdminRoleOrThrow(ctx);

        ChaincodeStub stub = ctx.getStub();
        if (!modelExists(ctx, modelId)) {
            String errorMessage = String.format("ModelMetadata %s doesn't exists", modelId);
            logger.log(Level.SEVERE, errorMessage);
            throw new ChaincodeException(errorMessage, ChaincodeErrors.MODEL_METADATA_DOES_NOT_EXISTS.toString());
        }

        String key = stub.createCompositeKey(MODEL_METADATA_KEY, modelId).toString();
        String json = stub.getStringState(key);
        FedAvgModelMetadata oldModelMetadata = FedAvgModelMetadata.deserialize(json);
        FedAvgModelMetadata newModelMetadata = new FedAvgModelMetadata(oldModelMetadata.getModelId(),
                oldModelMetadata.getName(),
                oldModelMetadata.getClientsPerRound(),
                MODEL_METADATA_STATUS_STARTED,
                oldModelMetadata.getTrainingRounds(),
                oldModelMetadata.getCurrentRound());
        String newJson = newModelMetadata.serialize();
        stub.putStringState(key, newJson);

        stub.setEvent(START_TRAINING_EVENT, newJson.getBytes());

        return newModelMetadata;
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public FedAvgModelMetadata createModelMetadata(final Context ctx, final String modelId,
                                             final String modelName, final String clientsPerRound, final String trainingRounds) {
        checkHasFlAdminRoleOrThrow(ctx);

        ChaincodeStub stub = ctx.getStub();

        if (modelExists(ctx, modelId)) {
            String errorMessage = String.format("ModelMetadata %s already exists", modelId);
            logger.log(Level.SEVERE, errorMessage);
            throw new ChaincodeException(errorMessage, ChaincodeErrors.MODEL_METADATA_ALREADY_EXISTS.toString());
        }

        FedAvgModelMetadata model = new FedAvgModelMetadata(modelId,
                modelName,
                Integer.parseInt(clientsPerRound),
                MODEL_METADATA_STATUS_INITIATED,
                Integer.parseInt(trainingRounds),
                0);

        String json = model.serialize();
        String key = stub.createCompositeKey(MODEL_METADATA_KEY, modelId).toString();
        stub.putStringState(key, json);

        stub.setEvent(CREATE_MODEL_METADATA_EVENT, json.getBytes());

        return model;
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public FedAvgModelMetadata getModelMetadata(final Context ctx, final String modelId) {
        ChaincodeStub stub = ctx.getStub();
        String modelMetadataJson = readJsonIfModelMetadataExistsOrThrow(stub, modelId);

        return FedAvgModelMetadata.deserialize(modelMetadataJson);
    }

    private static boolean notNullAndEmpty(final String str) {
        return str != null && !str.isEmpty();
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public EndRoundModel getTrainedModel(final Context ctx, final String modelId) {
        ChaincodeStub stub = ctx.getStub();
        String modelMetadataKey = stub.createCompositeKey(MODEL_METADATA_KEY, modelId).toString();
        String modelMetadataJson = stub.getStringState(modelMetadataKey);
        ModelMetadata metadata = ModelMetadata.deserialize(modelMetadataJson);
        if (!metadata.getStatus().equals(MODEL_METADATA_STATUS_FINISHED)) {
            String errorMessage = String.format("Training of ModelMetadata %s is not finished yet", modelId);
            logger.log(Level.SEVERE, errorMessage);
            throw new ChaincodeException(errorMessage, ChaincodeErrors.TRAINING_NOT_FINISHED.toString());
        }

        String key = stub.createCompositeKey(END_ROUND_MODEL_KEY,
                        modelId,
                        String.valueOf(metadata.getTrainingRounds()))
                .toString();
        String endRoundJson = stub.getStringState(key);
        return EndRoundModel.deserialize(endRoundJson);
    }

    // ------------------- FINISH ADMIN ---------------------

    // ------------------- START TRAINER ---------------------
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public FedAvgModelMetadata addOriginalModel(final Context ctx, final String modelId, final String weights, final String datasetSize) {
        checkHasTrainerRoleOrThrow(ctx);

        ChaincodeStub stub = ctx.getStub();

        String modelMetadataJson = readJsonIfModelMetadataExistsOrThrow(stub, modelId);
        FedAvgModelMetadata modelMetadata = FedAvgModelMetadata.deserialize(modelMetadataJson);

        int roundInt = modelMetadata.getCurrentRound();
        String roundStr = String.valueOf(roundInt);

        OriginalModel originalModel = new OriginalModel(modelId, roundInt, weights, Integer.parseInt(datasetSize));
        String json = originalModel.serialize();

        String username = getClientUsername(ctx);
        String key = stub.createCompositeKey(MODEL_KEY, modelId, roundStr, username).toString();

        stub.putStringState(key, json);

        byte[] event = new OriginalModel(originalModel.getModelId(),
                modelMetadata.getCurrentRound(),
                null,
                Integer.parseInt(datasetSize))
                .serialize()
                .getBytes();

        stub.setEvent(ORIGINAL_MODEL_ADDED_EVENT, event);

        logger.log(Level.INFO, "ModelUpdate " + key + " stored successfully");

        return modelMetadata;
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public EndRoundModel getEndRoundModel(final Context ctx, final String modelId) {
        ChaincodeStub stub = ctx.getStub();

        String modelMetadataJson = readJsonIfModelMetadataExistsOrThrow(stub, modelId);
        FedAvgModelMetadata modelMetadata = FedAvgModelMetadata.deserialize(modelMetadataJson);

        String previousRound = String.valueOf(modelMetadata.getCurrentRound() - 1);

        String key = stub.createCompositeKey(END_ROUND_MODEL_KEY, modelId, previousRound).toString();
        String endRoundModelJson = stub.getStringState(key);

        if (endRoundModelJson == null || endRoundModelJson.length() == 0) {
            String errorMessage = String.format("EndRoundModel not found. modelId: %s, round: %s", modelId, previousRound);
            logger.log(Level.SEVERE, errorMessage, endRoundModelJson);
            throw new ChaincodeException(errorMessage, ChaincodeErrors.INVALID_ACCESS.toString());
        }

        EndRoundModel endRoundModel = EndRoundModel.deserialize(endRoundModelJson);
        logger.log(Level.INFO, "EndRoundModel for round %s of model %s");

        return endRoundModel;
    }

    private void checkHasTrainerRoleOrThrow(final Context ctx) {
        if (checkHasTrainerAttribute(ctx)) {
            return;
        }
        String errorMessage = "User " + ctx.getClientIdentity().getId() + " has no trainer attribute";
        logger.log(Level.SEVERE, errorMessage);
        throw new ChaincodeException(errorMessage, ChaincodeErrors.INVALID_ACCESS.toString());
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public Boolean checkHasTrainerAttribute(final Context ctx) {
        return ctx.getClientIdentity().assertAttributeValue(TRAINER_ATTRIBUTE, Boolean.TRUE.toString());
    }

    // ------------------- FINISH TRAINER ---------------------

    // ------------------- START AGGREGATOR ---------------------
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    private int getNumberOfRequiredOriginalModels(final Context ctx, final String modelId) {
        String modelMetadataJson = readJsonIfModelMetadataExistsOrThrow(ctx.getStub(), modelId);
        FedAvgModelMetadata modelMetadata = FedAvgModelMetadata.deserialize(modelMetadataJson);
        return modelMetadata.getClientsPerRound();
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public int getNumberOfReceivedOriginalModels(final Context ctx, final String modelId) {
        //Do access control later
        ChaincodeStub stub = ctx.getStub();

        String modelMetadataJson = readJsonIfModelMetadataExistsOrThrow(stub, modelId);
        FedAvgModelMetadata modelMetadata = FedAvgModelMetadata.deserialize(modelMetadataJson);

        String round = String.valueOf(modelMetadata.getCurrentRound());
        String key = stub.createCompositeKey(MODEL_KEY, modelId, round).toString();
        QueryResultsIterator<KeyValue> results = stub.getStateByPartialCompositeKey(key);
        return countNumberOfValidValues(results);
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public Boolean checkAllOriginalModelsReceived(final Context ctx, final String modelId) throws Exception {
        //Do access control later
        int received = getNumberOfReceivedOriginalModels(ctx, modelId);
        int required = getNumberOfRequiredOriginalModels(ctx, modelId);

        if (received == required) {
            logger.log(Level.INFO, "Enough secrets are received.");
            return true;
        } else if (received > required) {
            logger.log(Level.SEVERE, "Something is wrong with secrets counter.");
        } else {
            logger.log(Level.INFO, "Looking for more secrets. Current value: " + received + " of " + required);
        }
        return false;
    }

    private int countNumberOfValidValues(final QueryResultsIterator<KeyValue> results) {
        int counter = 0;
        for (KeyValue result : results) {
            if (notNullAndEmpty(result.getStringValue())) {
                counter++;
            }
        }
        return counter;
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public FedAvgModelMetadata addEndRoundModel(final Context ctx, final String modelId, final String weights) {
        checkHasLeadAggregatorAttribute(ctx);

        ChaincodeStub stub = ctx.getStub();

        String modelMetadataJson = readJsonIfModelMetadataExistsOrThrow(stub, modelId);
        FedAvgModelMetadata modelMetadata = FedAvgModelMetadata.deserialize(modelMetadataJson);

        String round = String.valueOf(modelMetadata.getCurrentRound());

        EndRoundModel endRoundModel = new EndRoundModel(modelId, round, weights);
        String endRoundModelJson = endRoundModel.serialize();

        String key = stub.createCompositeKey(END_ROUND_MODEL_KEY, modelId, round).toString();
        stub.putStringState(key, endRoundModelJson);
        logger.log(Level.INFO, "ModelUpdate " + key + " stored successfully in public :)");

        boolean finishTraining = modelMetadata.getCurrentRound() >= modelMetadata.getTrainingRounds();
        String metadataKey = stub.createCompositeKey(MODEL_METADATA_KEY, modelId).toString();
        String status = finishTraining ? MODEL_METADATA_STATUS_FINISHED : MODEL_METADATA_STATUS_STARTED;
        FedAvgModelMetadata newModelMetadata = new FedAvgModelMetadata(modelMetadata.getModelId(),
                modelMetadata.getName(),
                modelMetadata.getClientsPerRound(),
                status,
                modelMetadata.getTrainingRounds(),
                modelMetadata.getCurrentRound() + 1);
        stub.putStringState(metadataKey, newModelMetadata.serialize());

        if (finishTraining) {
            stub.setEvent(TRAINING_FINISHED_EVENT, modelMetadataJson.getBytes());
            logger.log(Level.INFO, "Training finished successfully.");
        } else {
            stub.setEvent(ROUND_FINISHED_EVENT, modelMetadataJson.getBytes());
            logger.log(Level.INFO, "Round finished successfully.");
        }

        return newModelMetadata;
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public OriginalModelList getOriginalModelListForCurrentRound(final Context ctx, final String modelId) throws Exception {
        ChaincodeStub stub = ctx.getStub();

        String modelMetadataJson = readJsonIfModelMetadataExistsOrThrow(stub, modelId);
        FedAvgModelMetadata modelMetadata = FedAvgModelMetadata.deserialize(modelMetadataJson);

        return getOriginalModelList(ctx, modelId, String.valueOf(modelMetadata.getCurrentRound()));
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public OriginalModelList getOriginalModelList(final Context ctx, final String modelId, final String round) throws Exception {
        checkHasLeadAggregatorRoleOrThrow(ctx);

        ChaincodeStub stub = ctx.getStub();
        CompositeKey key = stub.createCompositeKey(MODEL_KEY, modelId, round);
        List<OriginalModel> originalModelList = new ArrayList<>();
        try (QueryResultsIterator<KeyValue> results = stub.getStateByPartialCompositeKey(key)) {
            for (KeyValue result : results) {
                if (!notNullAndEmpty(result.getStringValue())) {
                    logger.log(Level.SEVERE, "Invalid ModelUpdate json: %s\n", result.getStringValue());
                    continue;
                }
                OriginalModel originalModel = OriginalModel.deserialize(result.getStringValue());
                originalModelList.add(originalModel);
                logger.log(Level.INFO, String.format("Round %s of Model %s read successfully", originalModel.getRound(), originalModel.getModelId()));
            }
        }

        return new OriginalModelList(originalModelList);
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public Boolean checkHasLeadAggregatorAttribute(final Context ctx) {
        return ctx.getClientIdentity().assertAttributeValue(LEAD_AGGREGATOR_ATTRIBUTE, Boolean.TRUE.toString());
    }

    private void checkHasLeadAggregatorRoleOrThrow(final Context ctx) {
        if (checkHasLeadAggregatorAttribute(ctx)) {
            return;
        }
        String errorMessage = "User " + ctx.getClientIdentity().getId() + " has no leadAggregator attribute";
        logger.log(Level.SEVERE, errorMessage);
        throw new ChaincodeException(errorMessage, FedAvgChaincode.ChaincodeErrors.INVALID_ACCESS.toString());
    }
    // ------------------- FINISH AGGREGATOR ---------------------

    private String readJsonIfModelMetadataExistsOrThrow(final ChaincodeStub stub, final String modelId) {
        String key = stub.createCompositeKey(MODEL_METADATA_KEY, modelId).toString();
        String modelJson = stub.getStringState(key);
        if (modelJson == null || modelJson.isEmpty()) {
            String errorMessage = String.format("ModelMetadata %s does not exist", modelId);
            logger.log(Level.SEVERE, errorMessage);
            throw new ChaincodeException(errorMessage, ChaincodeErrors.MODEL_NOT_FOUND.toString());
        }
        logger.log(Level.INFO, "ModelMetadata Json: " + modelJson);
        return modelJson;
    }

    private void checkHasFlAdminRoleOrThrow(final Context ctx) {
        if (checkHasFlAdminAttribute(ctx)) {
            return;
        }
        String errorMessage = "User " + ctx.getClientIdentity().getId() + "has no admin attribute";
        logger.log(Level.SEVERE, errorMessage);
        throw new ChaincodeException(errorMessage, ChaincodeErrors.INVALID_ACCESS.toString());
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public Boolean checkHasFlAdminAttribute(final Context ctx) {
        return ctx.getClientIdentity().assertAttributeValue(FL_ADMIN_ATTRIBUTE, Boolean.TRUE.toString());
    }



    // ------------------- START GENERAL ---------------------
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public PersonalInfo getPersonalInfo(final Context ctx) {
        ChaincodeStub stub = ctx.getStub();

        String username = getClientUsername(ctx);
        String mspId = ctx.getClientIdentity().getMSPID();
        String role = getRole(ctx);

        String clientId = ctx.getClientIdentity().getId();
        if (role.equals(TRAINER_ATTRIBUTE)) {
            String key = stub.createCompositeKey(CLIENT_SELECTED_FOR_ROUND_KEY, username).toString();
            String value = stub.getStringState(key);
            Boolean selectedForRound = notNullAndEmpty(value) ? Boolean.TRUE : Boolean.FALSE;
            return new PersonalInfo(clientId, role, mspId, username, selectedForRound, null);
        }

        return new PersonalInfo(clientId, role, mspId, username, null, null);
    }

    private String getRole(final Context ctx) {
        if (checkHasLeadAggregatorAttribute(ctx)) {
            return LEAD_AGGREGATOR_ATTRIBUTE;
        } else if (checkHasTrainerAttribute(ctx)) {
            return TRAINER_ATTRIBUTE;
        }
        return "unknown";
    }
    // ------------------- FINISH GENERAL ---------------------


    private String getClientUsername(final Context ctx) {
        return ctx.getClientIdentity().getAttributeValue(ENROLMENT_ID_ATTRIBUTE_KEY);
    }

    private boolean modelExists(final Context ctx, final String modelId) {
        ChaincodeStub stub = ctx.getStub();
        String key = stub.createCompositeKey(MODEL_METADATA_KEY, modelId).toString();
        String modelJson = stub.getStringState(key);

        return (modelJson != null && !modelJson.isEmpty());
    }

    private void verifyClientOrgMatchesPeerOrg(final Context ctx) {
        String clientMSPID = ctx.getClientIdentity().getMSPID();
        String peerMSPID = ctx.getStub().getMspId();

        if (!peerMSPID.equals(clientMSPID)) {
            String errorMessage = String.format("Client from org %s is not authorized to read or write private data from an org %s peer", clientMSPID, peerMSPID);
            logger.log(Level.SEVERE, errorMessage);
            throw new ChaincodeException(errorMessage, ChaincodeErrors.INVALID_ACCESS.toString());
        }
    }

}
