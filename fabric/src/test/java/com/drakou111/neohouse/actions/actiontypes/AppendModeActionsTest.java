package com.drakou111.neohouse.actions.actiontypes;

import com.drakou111.neohouse.constants.AddAction;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AppendModeActionsTest {

    @Test
    void everyActionTypeIsRecognisedByItsName() {
        for (AddAction action : AddAction.values())
            assertThat(IfActionsFitAction.kindOf(action.displayName)).contains(action);
        assertThat(IfActionsFitAction.kindOf(" change variable ")).contains(AddAction.VARIABLE);
        assertThat(IfActionsFitAction.kindOf("Go Back")).isEmpty();
    }

    @Test
    void addingUpToTheLimitFits() {
        Map<AddAction, Integer> existing = Map.of(AddAction.CONDITIONAL, 23, AddAction.VARIABLE, 25);
        Map<AddAction, Integer> planned = Map.of(AddAction.CONDITIONAL, 2, AddAction.CHAT_MESSAGE, 20);

        assertThat(IfActionsFitAction.firstOverflow(existing, planned)).isEmpty();
    }

    @Test
    void goingOverALimitNamesTheTypeAndNumbers() {
        Map<AddAction, Integer> existing = Map.of(AddAction.CONDITIONAL, 25);
        Map<AddAction, Integer> planned = Map.of(AddAction.CONDITIONAL, 1);

        assertThat(IfActionsFitAction.firstOverflow(existing, planned))
                .contains("it has 25 'Conditional' actions and the script adds 1, over the limit of 25");
    }

    @Test
    void typesTheScriptDoesNotAddAreNotChecked() {
        // Housing allows only one of some actions; an existing full type doesn't block other ones.
        Map<AddAction, Integer> existing = Map.of(AddAction.EXIT, 1);

        assertThat(IfActionsFitAction.firstOverflow(existing, Map.of(AddAction.VARIABLE, 1))).isEmpty();
    }

    @Test
    void onlyEmptyMenuSlotsAreFilled() {
        List<Boolean> emptyElements = List.of(true, false, true, false, true, true, true, true, true);

        assertThat(FillEmptyMenuSlotsAction.slotsToFill(emptyElements, List.of(0, 1, 2, 3))).containsExactly(0, 2);
    }

    @Test
    void slotsPastTheEndOfASmallerMenuAreFilled() {
        List<Boolean> oneRow = List.of(false, false, false, false, false, false, false, false, false);

        assertThat(FillEmptyMenuSlotsAction.slotsToFill(oneRow, List.of(4, 9, 17))).containsExactly(9, 17);
    }
}
