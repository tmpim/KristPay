package pw.lemmmy.kristpay.commands;

import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import pw.lemmmy.kristpay.KristPay;
import pw.lemmmy.kristpay.krist.MasterWallet;

public class CommandMasterBal implements CommandExecutor {
	public static final CommandSpec SPEC = CommandSpec.builder()
		.description(Text.of("Check the KristPay master wallet's balance."))
		.permission("kristpay.command.masterbal.base")
		.executor(new CommandMasterBal())
		.build();
	
	@Override
	public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
		MasterWallet masterWallet = KristPay.INSTANCE.getMasterWallet();
		int masterBalance = masterWallet.getBalance();
		int allocated = KristPay.INSTANCE.getDatabase().getTotalDistributedKrist();
		float allocatedPercentage = (((float) allocated) / ((float) masterBalance)) * 100.0f;
		int unallocated = masterBalance - allocated;
		float unallocatedPercentage = (((float) unallocated) / ((float) masterBalance)) * 100.0f;
		
		src.sendMessage(
			Text.builder("Master wallet information:\n").color(TextColors.BLUE).append(
				Text.of(TextColors.AQUA, "Address: "),
				CommandHelpers.formatAddress(masterWallet),
				Text.of("\n"),
				Text.of(TextColors.AQUA, "Balance: "),
				CommandHelpers.formatKrist(masterBalance),
				Text.of("\n"),
				Text.of(TextColors.AQUA, "  Allocated: "),
				CommandHelpers.formatKrist(allocated),
				Text.of(TextColors.AQUA, " ("),
				Text.of(TextColors.GOLD, String.format("%.1f%%", allocatedPercentage)),
				Text.of(TextColors.AQUA, ")"),
				Text.of("\n"),
				Text.of(TextColors.AQUA, "  Unallocated: "),
				CommandHelpers.formatKrist(unallocated),
				Text.of(TextColors.AQUA, " ("),
				Text.of(TextColors.GOLD, String.format("%.1f%%", unallocatedPercentage)),
				Text.of(TextColors.AQUA, ")")
			).build()
		);
		
		return CommandResult.success();
	}
}
