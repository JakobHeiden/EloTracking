package de.neuefische.elotracking.backend.testfactories;

import de.neuefische.elotracking.backend.model.ChallengeModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static de.neuefische.elotracking.backend.testfactories.SnowflakeTestFactory.*;

public class ChallengeModelTestFactory {

    public static ChallengeModel create() {
        return new ChallengeModel(CHANNEL_ID, CHALLENGER_ID, ACCEPTOR_ID);
    }

    public static ChallengeModel createButReverseChallengerAndAcceptor() {
        return new ChallengeModel(CHANNEL_ID, ACCEPTOR_ID, CHALLENGER_ID);
    }

    public static List<ChallengeModel> createList(ChallengeModel... challenges) {
        List<ChallengeModel> list = new ArrayList<>();
        Arrays.stream(challenges).map(challenge -> list.add(challenge));
        return list;
    }
}
