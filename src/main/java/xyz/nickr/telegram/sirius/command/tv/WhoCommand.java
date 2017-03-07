package xyz.nickr.telegram.sirius.command.tv;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import pro.zackpollard.telegrambot.api.chat.message.Message;
import pro.zackpollard.telegrambot.api.chat.message.send.ParseMode;
import pro.zackpollard.telegrambot.api.chat.message.send.SendableTextMessage;
import xyz.nickr.telegram.sirius.Sirius;
import xyz.nickr.telegram.sirius.tv.Episode;
import xyz.nickr.telegram.sirius.tv.Season;
import xyz.nickr.telegram.sirius.tv.Series;
import xyz.nickr.telepad.TelepadBot;
import xyz.nickr.telepad.command.Command;
import xyz.nickr.telepad.util.PaginatedData;

/**
 * @author Nick Robson
 */
public class WhoCommand extends Command {

    public WhoCommand() {
        super("who");
        this.setUsage("(user)");
        this.setHelp("gets your (or someone's) progress on all series");
    }

    @Override
    public void exec(TelepadBot bot, Message message, String[] args) {
        long userId = message.getSender().getId();
        if (args.length > 0) {
            Long id = bot.getUserCache().getUserId(args[0]);
            if (id == null) {
                message.getChat().sendMessage(SendableTextMessage.plain("No user found matching that name.").replyTo(message).build());
                return;
            }
            userId = id;
        }
        Map<String, String> progress = Sirius.getProgressController().getProgress(userId);

        List<String> lines = new LinkedList<>();
        for (Map.Entry<String, String> entry : progress.entrySet()) {
            Series series = Sirius.getSeriesController().getSeries(entry.getKey(), true);
            int oldSize = lines.size();
            try {
                Map.Entry<Season, Episode> latestEpisode = series.getLastAiredEpisode();

                if (latestEpisode != null) {
                    String[] episodeParts = entry.getValue().substring(1).split("E");

                    int seasonId = Integer.parseInt(episodeParts[0]);
                    int episodeId = Integer.parseInt(episodeParts[1]);

                    int newSeasonId = latestEpisode.getKey().getId();

                    boolean added = false;
                    if (newSeasonId > seasonId) {
                        added = true;
                    } else if (newSeasonId == seasonId) {
                        Episode latestEp = latestEpisode.getValue();
                        if (latestEp.getId() > episodeId) {
                            added = true;
                        }
                    }

                    System.out.println("latest for " + series.getName() + ": S" + latestEpisode.getKey().getId() + "E" + latestEpisode.getValue().getId());

                    if (added) {
                        lines.add(String.format("*%s*: %s _(latest: S%sE%s)_", escape(series.getName(), false), escape(entry.getValue()), escape(String.valueOf(latestEpisode.getKey().getId()), false), escape(String.valueOf(latestEpisode.getValue().getId()), false)));
                    }
                }

            } catch (Exception ex) {
                ex.printStackTrace();
            }

            if (lines.size() == oldSize)
                lines.add(String.format("*%s*: %s", escape(series.getName(), false), escape(entry.getValue())));
        }
        lines.sort(String.CASE_INSENSITIVE_ORDER);

        PaginatedData paginatedData = new PaginatedData(lines, 15);
        paginatedData.setParseMode(ParseMode.MARKDOWN);
        paginatedData.send(0, message);
    }

}
