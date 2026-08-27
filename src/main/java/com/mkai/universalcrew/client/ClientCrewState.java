package com.mkai.universalcrew.client;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ClientCrewState {

    public static String crewName = "";

    public static Path logoPath = null;

    public static byte[] logoBytes =
            new byte[0];

    public static final List<Member> members =
            new ArrayList<>();

    private ClientCrewState() {
    }

    public record Member(
            UUID id,
            String name,
            String state
    ) {
    }

    public static void replaceMembers(
            List<Member> incoming
    ) {

        members.clear();
        members.addAll(incoming);
    }

    public static void clearCrew() {

        crewName = "";
        logoPath = null;
        logoBytes = new byte[0];
        members.clear();
    }
}