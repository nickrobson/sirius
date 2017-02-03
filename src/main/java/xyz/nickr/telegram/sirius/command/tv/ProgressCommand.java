package xyz.nickr.telegram.sirius.command.tv;

import java.util.regex.Pattern;
import pro.zackpollard.telegrambot.api.chat.message.Message;
import pro.zackpollard.telegrambot.api.chat.message.send.ParseMode;
import pro.zackpollard.telegrambot.api.chat.message.send.SendableTextMessage;
import xyz.nickr.jomdb.JavaOMDB;
import xyz.nickr.telegram.sirius.Sirius;
import xyz.nickr.telegram.sirius.tv.Series;
import xyz.nickr.telepad.TelepadBot;
import xyz.nickr.telepad.command.Command;

/**
 * @author Nick Robson
 */
public class ProgressCommand extends Command {

    public static Pattern EPISODE_PATTERN = Pattern.compile("^S0*[1-9][0-9]*E0*[1-9][0-9]*$");

    public ProgressCommand() {
        super("me");
        this.setHelp("gets and sets your progress on a series");
        this.setUsage("[series] (episode)");
    }

    @Override
    public void exec(TelepadBot bot, Message message, String[] args) {
        if (args.length != 1 && args.length != 2) {
            sendUsage(message);
            return;
        }
        Series series;
        if (JavaOMDB.IMDB_ID_PATTERN.matcher(args[0]).matches()) {
            series = Sirius.getSeriesController().getSeries(args[0], false);
        } else {
            series = Sirius.getSeriesController().getSeriesByLink(args[0]);
        }
        if (series == null) {
            message.getChat().sendMessage(
                    SendableTextMessage
                            .markdown("No such series!")
                            .replyTo(message)
                            .build());
            return;
        }
        String m;
        String progress = Sirius.getProgressController().getProgress(message.getSender(), series.getId());
        if (args.length == 1) {
            if (progress == null) {
                m = escape("You haven't said what episode you're up to!");
            } else {
                m = escape("You're up to ") + "*" + escape(progress) + "*" + escape(" for ") + "*" + escape(series.getName()) + "*";
            }
        } else {
            args[1] = args[1].toUpperCase();
            if (EPISODE_PATTERN.matcher(args[1]).matches()) {
                Sirius.getProgressController().setProgress(message.getSender(), series.getId(), args[1]);
                m = escape("Set progress to ") + "*" + escape(args[1]) + "*" + escape(" for ") + "*" + escape(series.getName()) + "*";
                if (progress != null) {
                    m += " _" + escape("(was " + progress + ")") + "_";
                }
            } else {
                m = "Invalid episode format, must be SxEy";
            }
        }
        message.getChat().sendMessage(
                SendableTextMessage.builder()
                        .replyTo(message)
                        .message(m)
                        .parseMode(ParseMode.MARKDOWN)
                        .build()
        );
    }

}
