package de.neuefische.elotracking.backend.command;

import de.neuefische.elotracking.backend.model.ChallengeModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static de.neuefische.elotracking.backend.command.SnowflakeTestFactory.*;

class ChallengeModelTestFactory {

    public static ChallengeModel create() {
        return new ChallengeModel(CHANNELID, CHALLENGERID, RECIPIENTID);
    }

    public static List<ChallengeModel> createList(ChallengeModel... challenges) {
        List<ChallengeModel> list = new ArrayList<>();
        Arrays.stream(challenges).map(challenge -> list.add(challenge));
        return list;
    }
}
