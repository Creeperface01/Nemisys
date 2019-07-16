package org.itxtech.nemisys.command.defaults;

import org.itxtech.nemisys.Nemisys;
import org.itxtech.nemisys.command.CommandSender;
import org.itxtech.nemisys.command.ConsoleCommandSender;
import org.itxtech.nemisys.math.NemisysMath;
import org.itxtech.nemisys.utils.TextFormat;

public class DeepDebugCommand extends VanillaCommand {

    public DeepDebugCommand(String name) {
        super(name, "enables deep debug", "/deepdebug");
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if(!(sender instanceof ConsoleCommandSender)) {
            return true;
        }

        Nemisys.DEEP_DEBUG = !Nemisys.DEEP_DEBUG;
        return true;
    }
}
