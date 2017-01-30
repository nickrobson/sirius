#!/usr/bin/env python3

import json
import os
import pymongo
import traceback

client = pymongo.MongoClient()
db = client['tvchatbot']

with open('shows.json', 'r') as f:
    data = json.loads(f.read())

shows = {}
profiles = {}
episodes = []

for show_data in data['shows']:
    shows[show_data['imdb']] = {'id': show_data['imdb'], 'name': show_data['display'], 'links': []}

for link_data in data['links']:
    show = shows.get(link_data['imdb'])
    if show is not None:
        show['links'].append(link_data['link'])

shows = list(map(lambda i: i[1], shows.items()))

try:
    db.shows.insert_many(shows)
except Exception as e:
    print('Exception while adding shows:')
    traceback.print_exc()
    print()
    print()

for fn in os.listdir('profiles'):
    with open(os.path.join('profiles', fn)) as f:
        profile_data = list(filter(lambda l: l.startswith('acc.Telegram'), f.read().splitlines()))
    if len(profile_data):
        profiles[fn.lower()] = profile_data[0].split('=')[1]

for fn in os.listdir('superchat_data'):
    id = fn[:-5]
    with open(os.path.join('superchat_data', fn)) as f:
        show_data = json.loads(f.read())
    for user, episode in show_data.items():
        uid = profiles.get(user.lower())
        if uid:
            episodes.append({'id': id, 'user': int(uid), 'episode': episode})

try:
    db.progress.insert_many(episodes)
except Exception as e:
    print('Exception while adding shows:')
    traceback.print_exc()
    print()
    print()
