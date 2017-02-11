package xyz.nickr.telegram.sirius.command.tv;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import pro.zackpollard.telegrambot.api.chat.message.Message;
import pro.zackpollard.telegrambot.api.chat.message.send.ParseMode;
import pro.zackpollard.telegrambot.api.chat.message.send.SendableTextMessage;
import xyz.nickr.jomdb.JavaOMDB;
import xyz.nickr.telegram.sirius.Sirius;
import xyz.nickr.telegram.sirius.tv.Episode;
import xyz.nickr.telegram.sirius.tv.Season;
import xyz.nickr.telegram.sirius.tv.Series;
import xyz.nickr.telepad.TelepadBot;
import xyz.nickr.telepad.command.Command;

/**
 * @author Nick Robson
 */
public class ProgressCommand extends Command {

    private static final Pattern EPISODE_PATTERN = Pattern.compile("^S0*([1-9][0-9]*)E0*([1-9][0-9]*)$");

    public ProgressCommand() {
        super("me");
        this.setHelp("gets and sets your progress on a series");
        this.setUsage("[series] (episode|remove|next (count))");
    }

    @Override
    public void exec(TelepadBot bot, Message message, String[] args) {
        if (args.length == 0) {
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
            reply(message, "No such series!", ParseMode.NONE);
            return;
        }
        String m = null;
        String progress = Sirius.getProgressController().getProgress(message.getSender(), series.getImdbId());
        if (args.length == 1) {
            if (progress == null) {
                m = escape("You haven't said what episode you're up to!");
            } else {
                m = escape("You're up to ") + "*" + escape(progress) + "*" + escape(" for ") + "*" + escape(series.getName()) + "*";
            }
        } else {
            args[1] = args[1].toUpperCase(bot.getLocale());
            String newProgress;
            if (bot.getCollator().equals("NEXT", args[1])) {
                if (progress == null) {
                    message.getChat().sendMessage(SendableTextMessage.plain("You haven't registered any progress yet!").replyTo(message).build());
                    return;
                }
                Matcher matcher = EPISODE_PATTERN.matcher(progress);
                int next = 1;
                if (args.length > 2)
                    next = Integer.valueOf(args[2]);
                if (matcher.matches()) {
                    newProgress = findNext(series, progress, next);
                    if (newProgress == null) {
                        message.getChat().sendMessage(SendableTextMessage.plain("I can't find any other episodes in season " + matcher.group(1) + ".").replyTo(message).build());
                        return;
                    }
                } else {
                    message.getChat().sendMessage(SendableTextMessage.plain("I had a problem with getting your past progress!").replyTo(message).build());
                    return;
                }
            } else if (bot.getCollator().equals("REMOVE", args[1])) {
                boolean deleted = Sirius.getProgressController().removeProgress(message.getSender(), series.getImdbId());
                if (deleted) {
                    reply(message, "Removed your progress on *" + escape(series.getName()) + "*!", ParseMode.MARKDOWN);
                } else {
                    reply(message, "No progress found on *" + escape(series.getName()) + "*!", ParseMode.MARKDOWN);
                }
                return;
            } else {
                Matcher matcher = EPISODE_PATTERN.matcher(args[1]);
                if (matcher.matches()) {
                    newProgress = String.format("S%sE%s", matcher.group(1), matcher.group(2));
                } else {
                    m = escape("Invalid episode format, must be SxEy");
                    newProgress = null;
                }
            }
            if (newProgress != null) {
                if (Objects.equals(progress, newProgress)) {
                    m = escape("You're already up to that!");
                } else {
                    Sirius.getProgressController().setProgress(message.getSender(), series.getImdbId(), newProgress);
                    m = escape("Set progress to ") + "*" + escape(newProgress) + "*" + escape(" for ") + "*" + escape(series.getName()) + "*";
                    if (progress != null) {
                        m += " _" + escape("(was " + progress + ")") + "_";
                    }
                }
            }
        }
        if (m != null)
            reply(message, m, ParseMode.MARKDOWN);
    }

    private String findPrevious(Series series, String current, int next) {
        if (next == 0)
            return current;
        if (next < 0)
            return findNext(series, current, -next);
        do {
            Matcher m = EPISODE_PATTERN.matcher(current);
            if (!m.matches())
                throw new IllegalArgumentException("not a valid episode string");
            int seasonId = Integer.valueOf(m.group(1));
            int episodeId = Integer.valueOf(m.group(2));
            Map.Entry<Season, Episode> ep = IntStream.range(0, series.getSeasons().length)
                    .mapToObj(i -> series.getSeasons()[series.getSeasons().length - i - 1])
                    .filter(Objects::nonNull)
                    .filter(s -> Integer.valueOf(s.getId()) <= seasonId)
                    .flatMap(s -> Arrays.stream(s.getEpisodes()).map(e -> new AbstractMap.SimpleEntry<>(s, e)))
                    .filter(e -> (Integer.valueOf(e.getKey().getId()) < seasonId) || (Integer.valueOf(e.getValue().getId()) < episodeId))
                    .findFirst()
                    .orElse(null);
            if (ep == null)
                return null;
            int newSeasonId = Integer.valueOf(ep.getKey().getId());
            int newEpisodeId;
            if (newSeasonId < seasonId)
                newEpisodeId = Integer.valueOf(ep.getKey().getEpisodes()[ep.getKey().getEpisodes().length - 1].getId());
            else {
                int diff = Math.min(next, episodeId - 1);
                newEpisodeId = episodeId - diff;
                next -= diff;
            }
            current = String.format("S%sE%s", newSeasonId, newEpisodeId);
        } while (--next > 0);
        return current;
    }

    private String findNext(Series series, String current, int next) {
        if (next <= 0)
            return findPrevious(series, current, -next);
        do {
            Matcher m = EPISODE_PATTERN.matcher(current);
            if (!m.matches())
                throw new IllegalArgumentException("not a valid episode string");
            int seasonId = Integer.valueOf(m.group(1));
            int episodeId = Integer.valueOf(m.group(2));
            Map.Entry<Season, Episode> ep = Arrays.stream(series.getSeasons())
                    .filter(Objects::nonNull)
                    .filter(s -> Integer.valueOf(s.getId()) >= seasonId)
                    .flatMap(s -> Arrays.stream(s.getEpisodes()).map(e -> new AbstractMap.SimpleEntry<>(s, e)))
                    .filter(e -> (Integer.valueOf(e.getKey().getId()) > seasonId) || (Integer.valueOf(e.getValue().getId()) > episodeId))
                    .findFirst()
                    .orElse(null);
            if (ep == null)
                return null;
            int newSeasonId = Integer.valueOf(ep.getKey().getId());
            int newEpisodeId;
            if (newSeasonId > seasonId)
                newEpisodeId = 1;
            else
                newEpisodeId = episodeId + 1;
            current = String.format("S%sE%s", newSeasonId, newEpisodeId);
        } while (--next > 0);
        return current;
    }

}
